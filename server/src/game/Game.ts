// Game gom 2 Player + Turn — orchestration: switchTurn, getOpponent, isFinished.
import { Player } from './Player.js';
import { GameStatus } from '@coincard/shared';
import { FIRST_PLAYER_HAND_SIZE, SECOND_PLAYER_HAND_SIZE } from './constants.js';

export class Game {
  private status: GameStatus = GameStatus.WAITING;
  private turn = 0;
  private activePlayerId = '';
  private winnerId: string | null = null;
  private statusMessage = '';
  private manualDrawTurn = -1;

  constructor(
    public readonly gameId: string,
    public readonly roomCode: string,
    private readonly players: [Player, Player],
  ) {}

  start(): void {
    if (this.status !== GameStatus.WAITING) throw new Error('Trận đã bắt đầu.');
    const firstPlayer = this.players[0];
    const secondPlayer = this.players[1];
    if (!firstPlayer || !secondPlayer) {
      throw new Error('Game cần đúng 2 player.');
    }
    firstPlayer.drawAndAddToHand(FIRST_PLAYER_HAND_SIZE);
    secondPlayer.drawAndAddToHand(SECOND_PLAYER_HAND_SIZE);
    // Đảm bảo tay đầu luôn có nước đi (lá ≤ 2 mana).
    firstPlayer.guaranteeCheapOpener(2);
    secondPlayer.guaranteeCheapOpener(2);
    this.activePlayerId = firstPlayer.id;
    this.turn = 1;
    this.status = GameStatus.PLAYING;
    this.setStatusMessage('HERO 1 STARTS');
    this.beginTurnForPlayer(0, { skipDraw: true });
  }

  switchTurn(): void {
    this.activePlayerId = this.getOpponent().id;
    this.turn++;
    this.setStatusMessage('');
    this.beginTurn();
  }

  isFinished(): boolean {
    return this.status === GameStatus.FINISHED;
  }

  getStatus(): GameStatus {
    return this.status;
  }

  getCurrentTurn(): number {
    return this.turn;
  }

  getActivePlayerId(): string {
    return this.activePlayerId;
  }

  getWinnerId(): string | null {
    return this.winnerId;
  }

  hasManualDrawnThisTurn(): boolean {
    return this.manualDrawTurn === this.turn;
  }

  markManualDraw(): void {
    this.manualDrawTurn = this.turn;
  }

  getStatusMessage(): string {
    return this.statusMessage;
  }

  setStatusMessage(message: string): void {
    this.statusMessage = message;
  }

  getPlayers(): Readonly<Player[]> {
    return this.players.slice();
  }

  getOpponent(): Player {
    return this.players.find((p) => p.id !== this.activePlayerId) as Player;
  }

  getPlayerById(playerId: string): Player {
    const found = this.players.find((p) => p.id === playerId);
    if (!found) {
      throw new Error('Player id không tồn tại.');
    }
    return found;
  }

  resign(playerId: string): void {
    this.getPlayerById(playerId);
    if (this.isFinished()) {
      return;
    }
    const winner = this.players.find((p) => p.id !== playerId);
    this.finish(winner?.id ?? null, 'CONCEDE');
  }

  finish(winnerId: string | null, reason = ''): void {
    if (this.isFinished()) {
      return;
    }
    this.status = GameStatus.FINISHED;
    this.winnerId = winnerId;
    if (reason) {
      this.setStatusMessage(reason);
    }
  }

  private beginTurn(): void {
    this.beginTurnForPlayer(this.players.findIndex((p) => p.id === this.activePlayerId));
  }

  private beginTurnForPlayer(index: number, options: { skipDraw?: boolean } = {}): void {
    const player = this.players[index];
    if (!player) {
      throw new Error('Player index không hợp lệ.');
    }
    player.increaseMaxMana();
    player.refillMana();
    player.getBoard().forEach((m) => m.startTurn());

    // Lượt mở màn không draw — tay đầu đúng spec 3/4 lá.
    if (options.skipDraw) {
      return;
    }
    if (player.deckSize > 0) player.addToHand(player.drawCard());
  }
}
