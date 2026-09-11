import type { Game } from './Game.js';
import type { Player } from './Player.js';
import { Minion } from './Minion.js';
import { makeUniqueCardId } from './Deck.js';
import { MAX_BOARD_SIZE } from './constants.js';
import { SILVER_HAND_RECRUIT_TOKEN as recruit } from './tokenCards.js';
import { BoardFullError, InvalidTargetError } from './errors.js';

export interface IHeroPower {
  validate(player: Player): void;
  execute(game: Game, player: Player): void;
}
class DamagePower implements IHeroPower {
  constructor(private readonly amount: number) {}
  validate(): void {}
  execute(game: Game, player: Player): void {
    player.recordDamage(game.getOpponent().heroState.takeDamage(this.amount));
  }
}
class RecruitPower implements IHeroPower {
  validate(player: Player): void {
    if (player.boardCount >= MAX_BOARD_SIZE) throw new BoardFullError();
  }
  execute(_game: Game, player: Player): void {
    player.summonMinion(new Minion(makeUniqueCardId(), recruit.id, recruit.name,
      recruit.attack, recruit.health, player.id, false, recruit.imagePath));
  }
}
class HealPower implements IHeroPower {
  validate(): void {}
  execute(_game: Game, player: Player): void { player.heroState.heal(2); }
}
class DrawPower implements IHeroPower {
  validate(): void {}
  execute(_game: Game, player: Player): void {
    player.heroState.takeDamage(2);
    if (player.deckSize > 0) player.addToHand(player.drawCard());
  }
}
const powers: Record<string, IHeroPower> = {
  MAGE: new DamagePower(1), HUNTER: new DamagePower(2),
  PALADIN: new RecruitPower(), PRIEST: new HealPower(), WARLOCK: new DrawPower(),
};
export function heroPowerFor(heroClass: string): IHeroPower {
  const power = powers[heroClass];
  if (!power) throw new InvalidTargetError('Hero chưa được hỗ trợ.');
  return power;
}
