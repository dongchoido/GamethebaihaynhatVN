// Room: tối đa 2 player, lưu session để reconnect.
import { RoomFullError } from '../game/errors.js';

export interface RoomPlayer {
  playerId: string;
  name: string;
  sessionToken: string;
  socketId: string | null;
  heroClass: string | null;
  ready: boolean;
}

export class Room {
  private players: RoomPlayer[] = [];
  private started = false;

  constructor(public readonly roomCode: string) {}

  addPlayer(player: RoomPlayer): void {
    if (this.players.length >= 2) {
      throw new RoomFullError();
    }
    this.players.push(player);
  }

  joinOrUpdate(player: RoomPlayer): void {
    const existing = this.players.find((p) => p.playerId === player.playerId);
    if (existing) {
      existing.socketId = player.socketId;
      return;
    }
    this.addPlayer(player);
  }

  getPlayers(): Readonly<RoomPlayer[]> {
    return this.players;
  }

  getPlayerBySession(sessionToken: string): RoomPlayer | null {
    return this.players.find((p) => p.sessionToken === sessionToken) ?? null;
  }

  count(): number {
    return this.players.length;
  }

  isFull(): boolean {
    return this.players.length === 2;
  }

  isStarted(): boolean {
    return this.started;
  }

  markStarted(): void {
    this.started = true;
  }

  removeSocket(socketId: string): RoomPlayer | null {
    const player = this.players.find((p) => p.socketId === socketId);
    if (!player) {
      return null;
    }
    player.socketId = null;
    return player;
  }
}
