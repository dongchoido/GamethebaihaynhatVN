import type { CardDefinition, HeroClass } from './cards.js';

export enum GameStatus {
  WAITING = 'WAITING',
  PLAYING = 'PLAYING',
  FINISHED = 'FINISHED',
}

export interface HeroState {
  heroId: string;
  name: string;
  heroClass: HeroClass;
  health: number;
  maxHealth: number;
  imagePath: string;
  powerName: string;
  powerCost: number;
}

export interface MinionState {
  instanceId: string;
  cardId: string;
  name: string;
  attack: number;
  health: number;
  maxHealth: number;
  canAttack: boolean;
  hasTaunt: boolean;
  imagePath: string;
}

export interface PlayerState {
  playerId: string;
  hero: HeroState;
  mana: number;
  maxMana: number;
  hand: CardDefinition[];
  handCount: number;
  board: MinionState[];
  deckCount: number;
  damageDealt: number;
  cardsPlayed: number;
  minionsSummoned: number;
  heroPowerUsed: boolean;
  fatigueDamage: number;
}

export interface GameState {
  gameId: string;
  roomCode: string;
  status: GameStatus;
  turn: number;
  activePlayerId: string;
  players: PlayerState[];
  winnerId: string | null;
  statusMessage: string;
}
