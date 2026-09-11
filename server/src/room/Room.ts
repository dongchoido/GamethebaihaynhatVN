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
  private starting = false;
  private lastActivityAt = Date.now();
  private readonly rematchVotes = new Set<string>();

  constructor(public readonly roomCode: string) {}

  addPlayer(player: RoomPlayer): void {
    if (this.players.length >= 2) {
      throw new RoomFullError();
    }
    this.players.push(player);
    this.touch();
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

  touch(): void {
    this.lastActivityAt = Date.now();
  }

  isIdle(ttlMs: number): boolean {
    return Date.now() - this.lastActivityAt >= ttlMs && this.players.every((p) => p.socketId === null);
  }

  tryStart(): boolean {
    if (this.started || this.starting) {
      return false;
    }
    this.starting = true;
    return true;
  }

  markStarted(): void {
    this.started = true;
    this.starting = false;
  }

  cancelStart(): void {
    this.starting = false;
  }

  removeSocket(socketId: string): RoomPlayer | null {
    const player = this.players.find((p) => p.socketId === socketId);
    if (!player) {
      return null;
    }
    player.socketId = null;
    this.touch();
    return player;
  }

  voteRematch(playerId: string): number {
    this.rematchVotes.add(playerId);
    this.touch();
    return this.rematchVotes.size;
  }

  clearRematchVotes(): void {
    this.rematchVotes.clear();
  }

  resetForRematch(): void {
    this.started = false;
    this.starting = false;
    this.clearRematchVotes();
    this.players.forEach((player) => {
      player.ready = true;
    });
    this.touch();
  }

  isStarting(): boolean {
    return this.starting;
  }

  getRematchVotes(): number {
    return this.rematchVotes.size;
  }

  // Khóa khởi tạo cho rematch: chống double-start khi 2 vote đến gần nhau,
  // và chống reset started khi trận mới đã chạy.
  tryStartRematch(): boolean {
    if (this.starting) {
      return false;
    }
    this.starting = true;
    this.started = false;
    this.clearRematchVotes();
    this.players.forEach((player) => {
      player.ready = true;
    });
    this.touch();
    return true;
  }
}
