import { Room } from './Room.js';
import { RoomNotFoundError } from '../game/errors.js';

export class RoomManager {
  private rooms = new Map<string, Room>();
  private sockets = new Map<string, Room>();
  private sessions = new Map<string, Room>();

  createRoom(): Room {
    const roomCode = this.generateCode();
    const room = new Room(roomCode);
    this.rooms.set(roomCode, room);
    return room;
  }

  getRoom(roomCode: string): Room {
    const upper = roomCode.toUpperCase().trim();
    const room = this.rooms.get(upper);
    if (!room) {
      throw new RoomNotFoundError();
    }
    return room;
  }

  findRoomBySocketId(socketId: string): Room | null {
    return this.sockets.get(socketId) ?? null;
  }

  findRoomBySessionToken(sessionToken: string): Room | null {
    return this.sessions.get(sessionToken) ?? null;
  }

  indexPlayer(room: Room, player: { socketId: string | null; sessionToken: string }): void {
    if (player.socketId) this.sockets.set(player.socketId, room);
    this.sessions.set(player.sessionToken, room);
  }

  unindexSocket(socketId: string): void {
    this.sockets.delete(socketId);
  }

  listRooms(): Room[] {
    return [...this.rooms.values()];
  }

  deleteRoom(roomCode: string): void {
    const room = this.rooms.get(roomCode.toUpperCase().trim());
    if (!room) return;
    room.getPlayers().forEach((player) => {
      if (player.socketId) this.sockets.delete(player.socketId);
      this.sessions.delete(player.sessionToken);
    });
    this.rooms.delete(room.roomCode);
  }

  private generateCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
  }
}
