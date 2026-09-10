import { randomUUID } from 'node:crypto';
import type { Server, Socket } from 'socket.io';
import type { Game } from '../game/Game.js';
import { GameEngine } from '../game/GameEngine.js';
import { Hero } from '../game/Hero.js';
import { Deck } from '../game/Deck.js';
import type { Player as DomainPlayer } from '../game/Player.js';
import type { Minion } from '../game/Minion.js';
import { RoomManager } from '../room/RoomManager.js';
import type { Room, RoomPlayer } from '../room/Room.js';
import type { PrismaClient } from '@prisma/client';
import {
  ServerEvents,
  type CardDefinition,
  type GameState,
  type PlayerState,
  type HeroState,
  type MinionState,
} from '@coincard/shared';
import type { IGameRepository } from '../database/repositories.js';
import { GameRuleError, ReconnectFailedError } from '../game/errors.js';
import { DECK_SIZE } from '../game/constants.js';

/**
 * GameService: nhận socket action → gọi engine → lưu lịch sử → broadcast state.
 * Không chứa rule gameplay (rule nằm trong GameEngine/domain).
 */
export class GameService {
  private readonly engine = new GameEngine();
  private readonly gameIndex = new Map<string, string>(); // roomCode → gameId

  constructor(
    private readonly io: Server,
    private readonly roomManager: RoomManager,
    private readonly gameRepository: IGameRepository,
    private readonly prisma: PrismaClient,
  ) {}

  public createRoom(socket: Socket, payload: { playerName: string }): void {
    const room = this.roomManager.createRoom();
    const token = randomUUID();
    const playerId = `player-${randomUUID()}`;
    room.addPlayer({
      playerId,
      name: payload.playerName,
      sessionToken: token,
      socketId: socket.id,
      heroClass: null,
      ready: false,
    });
    socket.join(room.roomCode);
    socket.emit(ServerEvents.ROOM_CREATED, {
      roomCode: room.roomCode,
      playerId,
      sessionToken: token,
      players: room.getPlayers().map((p) => ({ playerId: p.playerId, name: p.name })),
    });
  }

  public joinRoom(socket: Socket, payload: { roomCode: string; playerName: string }): void {
    const room = this.roomManager.getRoom(payload.roomCode);
    const token = randomUUID();
    const playerId = `player-${randomUUID()}`;
    room.addPlayer({
      playerId,
      name: payload.playerName,
      sessionToken: token,
      socketId: socket.id,
      heroClass: null,
      ready: false,
    });
    socket.join(room.roomCode);
    const players = room.getPlayers().map((p) => ({ playerId: p.playerId, name: p.name }));
    // Token chỉ gửi riêng cho người vừa join — không broadcast cho cả phòng.
    socket.emit(ServerEvents.PLAYER_JOINED, {
      roomCode: room.roomCode,
      playerId,
      sessionToken: token,
      players,
    });
    socket.to(room.roomCode).emit(ServerEvents.PLAYER_JOINED, {
      roomCode: room.roomCode,
      players,
    });

    if (room.isFull()) {
      this.io.to(room.roomCode).emit(ServerEvents.ROOM_READY, { roomCode: room.roomCode });
    }
  }

  public selectDeck(socket: Socket, payload: { heroId: string; roomCode: string }): void {
    const room = this.roomManager.getRoom(payload.roomCode);
    // Chống double-start khi click hero nhiều lần.
    if (room.isStarted()) {
      return;
    }
    const player = room.getPlayerBySession(this.tokenFrom(socket));
    if (!player) {
      throw new Error('Player không tồn tại trong room.');
    }
    player.heroClass = payload.heroId;
    player.ready = true;
    this.io.to(room.roomCode).emit(ServerEvents.DECK_SELECTED, {
      playerId: player.playerId,
      heroId: payload.heroId,
    });

    if (room.isFull() && room.getPlayers().every((p) => p.ready)) {
      void this.startGame(room);
    }
  }

  private async startGame(room: Room): Promise<void> {
    room.markStarted();
    const gameId = randomUUID();
    const heroes = await this.prisma.hero.findMany();
    const cardCatalog = await this.prisma.card.findMany({
      where: { collectible: true },
    });

    const players = room.getPlayers();
    const first = players[0];
    const second = players[1];
    if (!first || !second) {
      throw new Error('Room cần đủ 2 player để start.');
    }

    const game = this.engine.createGame(gameId, room.roomCode, [
      this.buildPlayerInit(first, heroes, cardCatalog),
      this.buildPlayerInit(second, heroes, cardCatalog),
    ]);
    game.start();
    this.gameIndex.set(room.roomCode, gameId);

    this.io.to(room.roomCode).emit(ServerEvents.GAME_STARTED, { gameId });
    this.broadcastState(room, game);
  }

  private buildPlayerInit(
    roomPlayer: RoomPlayer,
    heroes: Array<{ id: string; name: string; heroClass: string; powerName: string; powerCost: number; imagePath: string }>,
    cardCatalog: Array<{
      id: string;
      name: string;
      slug: string;
      description: string;
      type: string;
      rarity: string;
      manaCost: number;
      attack: number;
      health: number;
      heroClass: string;
      imagePath: string;
      effects: unknown;
      keywords: unknown;
    }>,
  ): { playerId: string; name: string; hero: Hero; deck: Deck } {
    const fallbackHero = heroes[0];
    if (!fallbackHero) {
      throw new Error('Chưa seed heroes.');
    }
    const heroRecord =
      heroes.find((h) => h.id === roomPlayer.heroClass || h.heroClass === roomPlayer.heroClass) ??
      fallbackHero;
    // Draft theo curve: ưu tiên lá rẻ để early game luôn có nước đi,
    // sau đó Deck tự Fisher-Yates shuffle.
    const deckCards = cardCatalog
      .slice()
      .sort((a, b) => a.manaCost - b.manaCost)
      .slice(0, DECK_SIZE)
      .map(
        (c) =>
          ({
            ...c,
            id: `${c.id}-${randomUUID()}`,
            effects: (c.effects as CardDefinition['effects']) ?? [],
            keywords: (c.keywords as CardDefinition['keywords']) ?? [],
            collectible: true,
          }) as CardDefinition,
      );
    return {
      playerId: roomPlayer.playerId,
      name: roomPlayer.name,
      hero: new Hero(
        heroRecord.id,
        heroRecord.name,
        heroRecord.heroClass,
        heroRecord.powerName,
        heroRecord.powerCost,
        heroRecord.imagePath,
      ),
      deck: new Deck(deckCards),
    };
  }

  public handleAction(
    socket: Socket,
    payload:
      | { gameId: string; cardInstanceId: string; targetId?: string }
      | { gameId: string; attackerId: string; targetId: string }
      | { gameId: string },
  ): void {
    const { gameId } = payload;
    const game = this.engine.getGame(gameId);
    const room = this.roomManager.getRoom(game.roomCode);

    try {
      const playerId = this.playerIdFromGame(game, socket);
      if ('cardInstanceId' in payload) {
        this.engine.playCard(gameId, playerId, payload.cardInstanceId, payload.targetId);
      } else if ('attackerId' in payload) {
        this.engine.attack(gameId, playerId, payload.attackerId, payload.targetId);
      } else {
        this.engine.endTurn(gameId, playerId);
      }
    } catch (error) {
      this.respondWithError(socket, error);
      return;
    }
    this.broadcastState(room, game);
  }

  public useHeroPower(socket: Socket, payload: { gameId: string }): void {
    const game = this.engine.getGame(payload.gameId);
    const room = this.roomManager.getRoom(game.roomCode);

    try {
      const playerId = this.playerIdFromGame(game, socket);
      this.engine.useHeroPower(payload.gameId, playerId);
    } catch (error) {
      this.respondWithError(socket, error);
      return;
    }
    this.broadcastState(room, game);
  }

  public concede(socket: Socket, payload: { gameId: string }): void {
    const game = this.engine.getGame(payload.gameId);
    const room = this.roomManager.getRoom(game.roomCode);

    try {
      const playerId = this.playerIdFromGame(game, socket);
      this.engine.concede(payload.gameId, playerId);
      void this.saveGame(game, room);
    } catch (error) {
      this.respondWithError(socket, error);
      return;
    }
    this.broadcastState(room, game);
  }

  public handleDisconnect(socket: Socket): void {
    const room = this.roomManager.findRoomBySocketId(socket.id);
    if (!room) {
      return;
    }
    const leftPlayer = room.removeSocket(socket.id);
    this.io.to(room.roomCode).emit(ServerEvents.PLAYER_DISCONNECTED, {
      playerId: leftPlayer?.playerId ?? socket.id,
    });
  }

  public reconnect(socket: Socket, payload: { sessionToken: string }): void {
    const room = this.roomManager.findRoomBySessionToken(payload.sessionToken);
    if (!room) {
      throw new ReconnectFailedError();
    }
    const player = room.getPlayerBySession(payload.sessionToken);
    if (!player) {
      throw new ReconnectFailedError();
    }
    player.socketId = socket.id;
    socket.join(room.roomCode);
    // Gửi lại danh sách phòng để client refresh ở lobby vẫn thấy đủ người.
    socket.emit(ServerEvents.PLAYER_JOINED, {
      roomCode: room.roomCode,
      players: room.getPlayers().map((p) => ({ playerId: p.playerId, name: p.name })),
    });

    const gameId = this.gameIndex.get(room.roomCode);
    if (gameId) {
      const game = this.engine.getGame(gameId);
      socket.emit(ServerEvents.GAME_STATE_UPDATED, {
        gameState: this.toStateFor(game, player.playerId),
      });
    }
  }

  // Che hand của đối thủ — mỗi viewer chỉ thấy bài của mình (server authoritative).
  private toStateFor(game: Game, viewerPlayerId: string): GameState {
    return {
      gameId: game.gameId,
      roomCode: game.roomCode,
      status: game.getStatus(),
      turn: game.getCurrentTurn(),
      activePlayerId: game.getActivePlayerId(),
      players: game.getPlayers().map((p) => this.playerToState(p, p.id === viewerPlayerId)),
      winnerId: game.getWinnerId(),
      statusMessage: game.getStatusMessage(),
    };
  }

  private playerToState(player: DomainPlayer, isOwner: boolean): PlayerState {
    return {
      playerId: player.id,
      hand: isOwner ? player.handCards.map((c) => ({ ...c })) : [],
      handCount: player.handCount,
      board: player.getBoard().map((m) => this.minionToState(m)),
      deckCount: player.deckSize,
      mana: player.currentMana,
      maxMana: player.currentMaxMana,
      hero: this.heroToState(player.heroState),
    };
  }

  private heroToState(hero: Hero): HeroState {
    return {
      heroId: hero.heroId,
      name: hero.name,
      heroClass: hero.heroClass,
      health: hero.currentHealth,
      maxHealth: hero.maxHealth,
      imagePath: hero.imagePath,
      powerName: hero.powerName,
      powerCost: hero.powerCost,
    };
  }

  private minionToState(minion: Minion): MinionState {
    return {
      instanceId: minion.instanceId,
      cardId: minion.cardId,
      name: minion.name,
      attack: minion.currentAttack,
      health: minion.currentHealth,
      maxHealth: minion.currentHealth,
      canAttack: minion.canAttack,
      imagePath: minion.imagePath,
    };
  }

  private broadcastState(room: Room, game: Game): void {
    // Gửi state riêng cho từng player để che hand đối thủ.
    for (const roomPlayer of room.getPlayers()) {
      if (!roomPlayer.socketId) {
        continue;
      }
      this.io
        .to(roomPlayer.socketId)
        .emit(ServerEvents.GAME_STATE_UPDATED, { gameState: this.toStateFor(game, roomPlayer.playerId) });
    }
    this.io.to(room.roomCode).emit(ServerEvents.TURN_CHANGED, {
      activePlayerId: game.getActivePlayerId(),
      turn: game.getCurrentTurn(),
    });

    if (game.isFinished()) {
      void this.saveGame(game, room);
      this.io.to(room.roomCode).emit(ServerEvents.GAME_OVER, {
        winnerId: game.getWinnerId(),
      });
    }
  }

  private async saveGame(game: Game, room: Room): Promise<void> {
    try {
      await this.gameRepository.recordFinishedGame({
        gameId: game.gameId,
        roomCode: room.roomCode,
        players: game.getPlayers().map((p) => ({
          playerId: p.id,
          name: p.name,
          winner: game.getWinnerId() === p.id,
        })),
        winnerId: game.getWinnerId(),
      });
    } catch {
      // Lịch sử fail không được làm sập game.
    }
  }

  private tokenFrom(socket: Socket): string {
    const token = socket.handshake.auth.sessionToken;
    return typeof token === 'string' ? token : '';
  }

  private playerIdFromGame(game: Game, socket: Socket): string {
    const room = this.roomManager.getRoom(game.roomCode);
    const roomPlayer = room.getPlayerBySession(this.tokenFrom(socket));
    if (!roomPlayer) {
      throw new Error('Player không tồn tại trong room.');
    }
    return roomPlayer.playerId;
  }

  private respondWithError(socket: Socket, error: unknown): void {
    if (error instanceof GameRuleError) {
      socket.emit(ServerEvents.ACTION_REJECTED, {
        code: error.code,
        message: error.message,
      });
      return;
    }
    socket.emit(ServerEvents.ACTION_REJECTED, {
      code: 'UNKNOWN',
      message: error instanceof Error ? error.message : 'Unknown error.',
    });
  }
}
