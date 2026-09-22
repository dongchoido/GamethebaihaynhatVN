import type { CardDefinition, HeroClass } from './cards.js';
import type { GameState } from './game.js';
import { ClientEvents, ServerEvents } from '../events/socketEvents.js';

export interface CreateRoomPayload {
  playerName: string;
}

export interface JoinRoomPayload {
  roomCode: string;
  playerName: string;
}

export interface SubmitLoadoutPayload {
  roomCode: string;
  heroClass: HeroClass;
  cardSlugs: string[];
}

export interface PlayCardPayload {
  gameId: string;
  cardInstanceId: string;
  targetId?: string;
}

export interface AttackPayload {
  gameId: string;
  attackerId: string;
  targetId: string;
}

export interface GameActionPayload {
  gameId: string;
}

export interface ReconnectGamePayload {
  sessionToken: string;
}

export interface RoomPlayerState {
  playerId: string;
  name: string;
  heroClass: HeroClass | null;
  deckReady: boolean;
  ready: boolean;
  connected: boolean;
}

export interface RoomCreatedResponse {
  roomCode: string;
  playerId: string;
  sessionToken: string;
  players: RoomPlayerState[];
}

export interface PlayerJoinedResponse {
  roomCode: string;
  playerId: string | null;
  sessionToken: string | null;
  players: RoomPlayerState[];
}

export interface RoomReadyResponse {
  roomCode: string;
}

export interface LoadoutAcceptedResponse {
  roomCode: string;
  players: RoomPlayerState[];
}

export interface GameStartedResponse {
  gameId: string;
}

export const ERROR_CODES = [
  'NOT_PLAYER_TURN',
  'NOT_ENOUGH_MANA',
  'INVALID_TARGET',
  'BOARD_FULL',
  'CARD_NOT_IN_HAND',
  'INVALID_DECK',
  'ROOM_FULL',
  'ROOM_NOT_FOUND',
  'RECONNECT_FAILED',
  'INVALID_PAYLOAD',
  'INVALID_COMMAND',
  'INTERNAL_ERROR',
  'GAME_NOT_RUNNING',
  'HERO_POWER_ALREADY_USED',
  'TAUNT_REQUIRED',
  'GAME_START_FAILED',
  'REMATCH_NOT_ALLOWED',
  'REMATCH_IN_PROGRESS',
  'REMATCH_FAILED',
] as const;

export type ErrorCode = (typeof ERROR_CODES)[number];

export interface ActionRejectedResponse {
  code: ErrorCode;
  message: string;
}

export interface GameOverResponse {
  winnerId: string | null;
}

export interface PlayerDisconnectedResponse {
  playerId: string;
}

export interface GameStatePayload {
  gameState: GameState;
}

export interface GameCatalogResponse {
  heroes: HeroCatalog[];
  collectibleCards: CardDefinition[];
  deckRules: DeckRules;
  suggestedDecks: Partial<Record<HeroClass, string[]>>;
}

export interface HeroCatalog {
  id: string;
  name: string;
  heroClass: HeroClass;
  powerName: string;
  powerCost: number;
  imagePath: string;
}

export interface DeckRules {
  deckSize: number;
  maxCopies: number;
  maxLegendaryCopies: number;
  heroClasses: HeroClass[];
}

export interface ClientEventPayloads {
  [ClientEvents.CREATE_ROOM]: CreateRoomPayload;
  [ClientEvents.JOIN_ROOM]: JoinRoomPayload;
  [ClientEvents.SUBMIT_LOADOUT]: SubmitLoadoutPayload;
  [ClientEvents.PLAY_CARD]: PlayCardPayload;
  [ClientEvents.ATTACK]: AttackPayload;
  [ClientEvents.END_TURN]: GameActionPayload;
  [ClientEvents.USE_HERO_POWER]: GameActionPayload;
  [ClientEvents.CONCEDE]: GameActionPayload;
  [ClientEvents.RECONNECT_GAME]: ReconnectGamePayload;
  [ClientEvents.REMATCH]: GameActionPayload;
}

export type ClientEventPayload<Event extends ClientEvents> = ClientEventPayloads[Event];

export interface ServerEventPayloads {
  [ServerEvents.ROOM_CREATED]: RoomCreatedResponse;
  [ServerEvents.PLAYER_JOINED]: PlayerJoinedResponse;
  [ServerEvents.ROOM_READY]: RoomReadyResponse;
  [ServerEvents.LOADOUT_ACCEPTED]: LoadoutAcceptedResponse;
  [ServerEvents.GAME_STARTED]: GameStartedResponse;
  [ServerEvents.GAME_STATE_UPDATED]: GameStatePayload;
  [ServerEvents.ACTION_REJECTED]: ActionRejectedResponse;
  [ServerEvents.GAME_OVER]: GameOverResponse;
  [ServerEvents.PLAYER_DISCONNECTED]: PlayerDisconnectedResponse;
}

export type ServerEventPayload<Event extends ServerEvents> = ServerEventPayloads[Event];
