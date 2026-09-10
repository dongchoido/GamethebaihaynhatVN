import { Game } from './Game.js';
import { Player } from './Player.js';
import { Hero } from './Hero.js';
import { Deck, makeUniqueCardId } from './Deck.js';
import { Minion } from './Minion.js';
import { GameStatus, CardType } from '@coincard/shared';
import { EffectResolver } from './EffectResolver.js';
import { resolveAttack } from './CombatService.js';
import { MAX_BOARD_SIZE } from './constants.js';
import { SILVER_HAND_RECRUIT_TOKEN } from './tokenCards.js';
import {
  NotPlayerTurnError,
  NotEnoughManaError,
  BoardFullError,
  GameNotRunningError,
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
    const player = game.getPlayerById(playerId);

    this.assertActivePlayer(game, playerId);

    const card = player.removeFromHand(cardInstanceId);

    if (card.manaCost > player.currentMana) {
      player.addToHand(card);
      throw new NotEnoughManaError();
    }
    player.spendMana(card.manaCost);

    if (card.type === CardType.MINION) {
      if (player.boardCount >= MAX_BOARD_SIZE) {
        player.addToHand(card);
        player.increaseMaxMana(); // rollback đơn giản: hoàn mana đã trừ
        player.refillMana();
        throw new BoardFullError();
      }
      const minion = new Minion(
        makeUniqueCardId(),
        card.id,
        card.name,
        card.attack,
        card.health,
        player.id,
        card.keywords.includes('CHARGE'),
        card.imagePath,
      );
      player.summonMinion(minion);
    } else {
      // Spell — resolve từng effect, sau đó dọn minion chết.
      for (const effect of card.effects) {
        this.resolver.resolve(game, player, game.getOpponent(), effect, targetId);
      }
      player.removeDeadMinions();
      game.getOpponent().removeDeadMinions();
    }

    this.checkWinner(game);
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

  useHeroPower(gameId: string, playerId: string): void {
    const game = this.ensurePlaying(gameId);
    const player = game.getPlayerById(playerId);
    const hero = player.heroState;
    this.assertActivePlayer(game, playerId);

    if (player.currentMana < hero.powerCost) {
      throw new NotEnoughManaError();
    }
    player.spendMana(hero.powerCost);

    switch (hero.heroClass) {
      case 'MAGE':
        // Fireblast — 1 damage tới opponent hero
        game.getOpponent().heroState.takeDamage(1);
        break;
      case 'HUNTER':
        // Steady Shot — 2 damage tới opponent hero
        game.getOpponent().heroState.takeDamage(2);
        break;
      case 'PALADIN':
        // Reinforce — summon 1/1 Silver Hand Recruit
        player.summonMinion(
          new Minion(
            makeUniqueCardId(),
            SILVER_HAND_RECRUIT_TOKEN.id,
            SILVER_HAND_RECRUIT_TOKEN.name,
            SILVER_HAND_RECRUIT_TOKEN.attack,
            SILVER_HAND_RECRUIT_TOKEN.health,
            player.id,
            false,
            SILVER_HAND_RECRUIT_TOKEN.imagePath,
          ),
        );
        break;
      case 'PRIEST':
        // Lesser Heal — heal 2 HP hero mình
        hero.heal(2);
        break;
      case 'WARLOCK':
        // Life Tap — chịu 2 damage + draw 1 lá
        hero.takeDamage(2);
        try {
          player.addToHand(player.drawCard());
        } catch {
          // fatigue
        }
        break;
      default:
        break;
    }
    this.checkWinner(game);
  }

  concede(gameId: string, playerId: string): void {
    const game = this.getGame(gameId);
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

  private checkWinner(game: Game): void {
    for (const p of game.getPlayers()) {
      if (p.heroState.isDead()) {
        const winner = game.getPlayers().find((x) => x.id !== p.id);
        game.finish(winner?.id ?? null);
        return;
      }
    }
  }
}
