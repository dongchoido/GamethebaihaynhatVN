import { Room } from './Room.js';
import { RoomNotFoundError } from '../game/errors.js';

export class RoomManager {
  private rooms = new Map<string, Room>();

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
    for (const room of this.rooms.values()) {
      if (room.getPlayers().some((p) => p.socketId === socketId)) {
        return room;
      }
    }
    return null;
  }

  findRoomBySessionToken(sessionToken: string): Room | null {
    for (const room of this.rooms.values()) {
      if (room.getPlayerBySession(sessionToken)) {
        return room;
      }
    }
    return null;
  }

  private generateCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
  }
}
