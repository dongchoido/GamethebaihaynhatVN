import type { GameState } from './game.js';

export interface JoinRoomPayload {
  roomCode: string;
  playerName: string;
}

export interface RoomCreatedResponse {
  roomCode: string;
  playerId: string;
  sessionToken: string;
  players: { playerId: string; name: string; heroClass?: string | null; ready?: boolean; connected?: boolean }[];
}

export interface PlayerJoinedResponse {
  roomCode: string;
  // Chỉ có khi event gửi riêng cho người vừa join (không broadcast token cho cả phòng).
  playerId?: string;
  sessionToken?: string;
  players: { playerId: string; name: string; heroClass?: string | null; ready?: boolean; connected?: boolean }[];
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

export interface ActionRejectedResponse {
  code: string;
  message: string;
}

export interface GameOverResponse {
  winnerId: string | null;
}

export interface GameStatePayload {
  gameState: GameState;
}
