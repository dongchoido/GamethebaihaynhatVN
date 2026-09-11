import { Game } from './Game.js';
import { Player } from './Player.js';
import { Hero } from './Hero.js';
import { Deck, makeUniqueCardId } from './Deck.js';
import { Minion } from './Minion.js';
import { GameStatus, CardType, effectNeedsTarget } from '@coincard/shared';
import { EffectResolver } from './EffectResolver.js';
import { resolveAttack } from './CombatService.js';
import { MAX_BOARD_SIZE, MAX_HAND_SIZE } from './constants.js';
import { heroPowerFor } from './HeroPower.js';
import {
  NotPlayerTurnError,
  NotEnoughManaError,
  BoardFullError,
  CardNotInHandError,
  GameNotRunningError,
  HandFullError,
  DeckEmptyError,
  AlreadyDrewError,
} from './errors.js';

export class GameEngine {
  private games = new Map<string, Game>();
  private readonly resolver = new EffectResolver();

  /** Tạo game mới từ 2 player đã chọn hero + deck. */
  createGame(
    gameId: string,
    roomCode: string,
    players: [
      { playerId: string; name: string; hero: Hero; deck: Deck },
      { playerId: string; name: string; hero: Hero; deck: Deck },
    ],
  ): Game {
    const [p1, p2] = players;
    const game = new Game(gameId, roomCode, [
      new Player(p1.playerId, p1.name, p1.hero, p1.deck),
      new Player(p2.playerId, p2.name, p2.hero, p2.deck),
    ]);
    this.games.set(gameId, game);
    return game;
  }

  startGame(gameId: string): Game {
    const game = this.getGame(gameId);
    game.start();
    return game;
  }

  playCard(
    gameId: string,
    playerId: string,
    cardInstanceId: string,
    targetId?: string,
  ): void {
    const game = this.ensurePlaying(gameId);
    this.assertActivePlayer(game, playerId);
    const player = game.getPlayerById(playerId);
    const opponent = game.getOpponent();

    // ===== PHASE 1: VALIDATE — mọi điều kiện kiểm tra trước, không mutation.
    const card = player.findCardInHand(cardInstanceId);
    if (!card) {
      throw new CardNotInHandError();
    }
    if (card.manaCost > player.currentMana) {
      throw new NotEnoughManaError();
    }
    if (card.type === CardType.MINION && player.boardCount >= MAX_BOARD_SIZE) {
      throw new BoardFullError();
    }
    if (card.type !== CardType.MINION) {
      for (const effect of card.effects) {
        // targetId của client chỉ dành cho effect cần chọn tay (vd DESTROY);
        // effect tự resolve (HEAL hero, AOE...) nhận undefined để tự tìm target.
        this.resolver.validate(player, opponent, effect, this.targetForEffect(effect, targetId));
      }
    }

    // ===== PHASE 2: COMMIT — đã validate xong, thực thi nguyên tử.
    const undo = game.getPlayers().map(p => p.checkpoint());
    try {
    player.removeFromHand(cardInstanceId);
    player.spendMana(card.manaCost);
    player.recordCardPlayed();

    if (card.type === CardType.MINION) {
      const minion = new Minion(
        makeUniqueCardId(),
        card.id,
        card.name,
        card.attack,
        card.health,
        player.id,
        card.keywords.includes('CHARGE'),
        card.imagePath,
        card.keywords.includes('TAUNT'),
      );
      player.summonMinion(minion);
    } else {
      for (const effect of card.effects) {
        this.resolver.resolve(game, player, opponent, effect, this.targetForEffect(effect, targetId));
      }
      player.removeDeadMinions();
      opponent.removeDeadMinions();
    }

    this.checkWinner(game);
    } catch (error) {
      undo.forEach(restore => restore());
      throw error;
    }
  }

  attack(
    gameId: string,
    playerId: string,
    attackerId: string,
    targetId: string,
  ): void {
    const game = this.ensurePlaying(gameId);
    const player = game.getPlayerById(playerId);
    const opponent = game.getOpponent();

    this.assertActivePlayer(game, playerId);
    resolveAttack(player, opponent, attackerId, targetId);

    player.removeDeadMinions();
    opponent.removeDeadMinions();
    this.checkWinner(game);
  }

  endTurn(gameId: string, playerId: string): void {
    const game = this.ensurePlaying(gameId);
    this.assertActivePlayer(game, playerId);
    game.switchTurn();
  }

  // Rút 1 lá thủ công từ bộ bài — mỗi turn 1 lần, tay tối đa 6 lá.
  drawCard(gameId: string, playerId: string): void {
    const game = this.ensurePlaying(gameId);
    this.assertActivePlayer(game, playerId);
    const player = game.getPlayerById(playerId);
    if (player.handCount >= MAX_HAND_SIZE) {
      throw new HandFullError();
    }
    if (player.deckSize <= 0) {
      throw new DeckEmptyError();
    }
    if (game.hasManualDrawnThisTurn()) {
      throw new AlreadyDrewError();
    }
    player.addToHand(player.drawCard());
    game.markManualDraw();
  }

  useHeroPower(gameId: string, playerId: string): void {
    const game = this.ensurePlaying(gameId);
    const player = game.getPlayerById(playerId);
    const hero = player.heroState;
    this.assertActivePlayer(game, playerId);

    if (player.currentMana < hero.powerCost) {
      throw new NotEnoughManaError();
    }
    const power = heroPowerFor(hero.heroClass);
    power.validate(player);
    const undo = game.getPlayers().map(p => p.checkpoint());
    try {
      player.spendMana(hero.powerCost);
      power.execute(game, player);
      this.checkWinner(game);
    } catch (error) {
      undo.forEach(restore => restore());
      throw error;
    }
  }

  concede(gameId: string, playerId: string): void {
    const game = this.ensurePlaying(gameId);
    game.resign(playerId);
  }

  getGame(gameId: string): Game {
    const game = this.games.get(gameId);
    if (!game) {
      throw new Error('Game không tồn tại.');
    }
    return game;
  }

  removeGame(gameId: string): void {
    this.games.delete(gameId);
  }

  private ensurePlaying(gameId: string): Game {
    const game = this.getGame(gameId);
    if (game.getStatus() !== GameStatus.PLAYING) {
      throw new GameNotRunningError();
    }
    return game;
  }

  private assertActivePlayer(game: Game, playerId: string): void {
    if (game.getActivePlayerId() !== playerId) {
      throw new NotPlayerTurnError();
    }
  }

  // Lá multi-effect (vd Siphon Soul): targetId client gửi chỉ áp dụng cho
  // effect cần chọn tay; effect tự resolve nhận undefined để tự tìm target.
  private targetForEffect(effect: { target: string }, targetId?: string): string | undefined {
    return effectNeedsTarget(effect.target as Parameters<typeof effectNeedsTarget>[0])
      ? targetId
      : undefined;
  }

  private checkWinner(game: Game): void {
    if (game.getPlayers().every(p => p.heroState.isDead())) {
      game.finish(null, 'DRAW');
      return;
    }
    for (const p of game.getPlayers()) {
      if (p.heroState.isDead()) {
        const winner = game.getPlayers().find((x) => x.id !== p.id);
        game.finish(winner?.id ?? null);
        return;
      }
    }
  }
}
