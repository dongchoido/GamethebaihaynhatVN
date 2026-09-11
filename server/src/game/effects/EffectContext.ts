import type { Game } from '../Game.js';
import type { Minion } from '../Minion.js';
import type { Player } from '../Player.js';

// Polymorphism context — mọi effect cùng giải quyết qua execute(context).
export interface EffectContext {
  game: Game;
  player: Player;
  opponent: Player;
  target?: Minion | Player | null;
  value: number;
  areaTargets?: readonly Minion[];
}
