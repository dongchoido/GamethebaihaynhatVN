import type { Server, Socket } from 'socket.io';
import { RoomManager } from '../room/RoomManager.js';
import { GameService } from './GameService.js';
import { PrismaGameRepository } from '../database/repositories.js';
import { prisma } from '../database/prismaClient.js';
import { GameRuleError } from '../game/errors.js';
import {
  ClientEvents, ServerEvents,
  type CreateRoomPayload, type JoinRoomPayload,
  type SelectDeckPayload, type PlayCardPayload, type AttackPayload,
  type ReconnectPayload,
} from '@coincard/shared';

/**
 * SocketHandler — chỉ nhận/validate format rồi giao cho GameService.
 * Không chứa business logic.
 *
 * Mọi handler đều bọc safe(): lỗi client (sai mã phòng, phòng đầy, sai turn...)
 * trả về ACTION_REJECTED — KHÔNG BAO GIỜ để exception giết server.
 */
export function registerSocketHandler(io: Server): void {
  const roomManager = new RoomManager();
  const service = new GameService(io, roomManager, new PrismaGameRepository(prisma), prisma);

  const safe = (socket: Socket, fn: () => void): void => {
    try {
      fn();
    } catch (error) {
      if (error instanceof GameRuleError) {
        socket.emit(ServerEvents.ACTION_REJECTED, {
          code: error.code,
          message: error.message,
        });
        return;
      }
      socket.emit(ServerEvents.ACTION_REJECTED, {
        code: 'UNKNOWN',
        message: error instanceof Error ? error.message : 'Unknown error.',
      });
    }
  };

  io.on('connection', (socket: Socket) => {
    socket.on(ClientEvents.CREATE_ROOM, (payload: CreateRoomPayload) => {
      safe(socket, () => service.createRoom(socket, payload));
    });

    socket.on(ClientEvents.JOIN_ROOM, (payload: JoinRoomPayload) => {
      safe(socket, () => service.joinRoom(socket, payload));
    });

    socket.on(ClientEvents.SELECT_DECK, (payload: SelectDeckPayload & { roomCode: string }) => {
      safe(socket, () => service.selectDeck(socket, payload));
    });

    socket.on(ClientEvents.PLAY_CARD, (payload: PlayCardPayload) => {
      safe(socket, () => service.handleAction(socket, payload));
    });

    socket.on(ClientEvents.ATTACK, (payload: AttackPayload) => {
      safe(socket, () => service.handleAction(socket, payload));
    });

    socket.on(ClientEvents.END_TURN, (payload: { gameId: string }) => {
      safe(socket, () => service.handleAction(socket, payload));
    });

    socket.on(ClientEvents.USE_HERO_POWER, (payload: { gameId: string }) => {
      safe(socket, () => service.useHeroPower(socket, payload));
    });

    socket.on(ClientEvents.CONCEDE, (payload: { gameId: string }) => {
      safe(socket, () => service.concede(socket, payload));
    });

    socket.on(ClientEvents.RECONNECT_GAME, (payload: ReconnectPayload) => {
      safe(socket, () => service.reconnect(socket, payload));
    });

    socket.on('disconnect', () => {
      safe(socket, () => service.handleDisconnect(socket));
    });
  });
}
