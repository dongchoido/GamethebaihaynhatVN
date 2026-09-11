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
import { objectPayload, optionalString, requiredString } from './validation.js';

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
  service.startCleanup();

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
      safe(socket, () => {
        const body = objectPayload(payload);
        service.createRoom(socket, { playerName: requiredString(body, 'playerName', 24) });
      });
    });

    socket.on(ClientEvents.JOIN_ROOM, (payload: JoinRoomPayload) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.joinRoom(socket, {
          roomCode: requiredString(body, 'roomCode', 6),
          playerName: requiredString(body, 'playerName', 24),
        });
      });
    });

    socket.on(ClientEvents.SELECT_DECK, (payload: SelectDeckPayload & { roomCode: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.selectDeck(socket, {
          heroId: requiredString(body, 'heroId', 32),
          roomCode: requiredString(body, 'roomCode', 6),
        });
      });
    });

    socket.on(ClientEvents.PLAY_CARD, (payload: PlayCardPayload) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.handleAction(socket, {
          gameId: requiredString(body, 'gameId', 64),
          cardInstanceId: requiredString(body, 'cardInstanceId', 128),
          targetId: optionalString(body, 'targetId', 128),
        });
      });
    });

    socket.on(ClientEvents.ATTACK, (payload: AttackPayload) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.handleAction(socket, {
          gameId: requiredString(body, 'gameId', 64),
          attackerId: requiredString(body, 'attackerId', 128),
          targetId: requiredString(body, 'targetId', 128),
        });
      });
    });

    socket.on(ClientEvents.END_TURN, (payload: { gameId: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.handleAction(socket, { gameId: requiredString(body, 'gameId', 64) });
      });
    });

    socket.on(ClientEvents.USE_HERO_POWER, (payload: { gameId: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.useHeroPower(socket, { gameId: requiredString(body, 'gameId', 64) });
      });
    });

    socket.on(ClientEvents.DRAW_CARD, (payload: { gameId: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.drawCard(socket, { gameId: requiredString(body, 'gameId', 64) });
      });
    });

    socket.on(ClientEvents.CONCEDE, (payload: { gameId: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.concede(socket, { gameId: requiredString(body, 'gameId', 64) });
      });
    });

    socket.on(ClientEvents.RECONNECT_GAME, (payload: ReconnectPayload) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.reconnect(socket, { sessionToken: requiredString(body, 'sessionToken', 128) });
      });
    });

    socket.on(ClientEvents.REMATCH, (payload: { gameId: string }) => {
      safe(socket, () => {
        const body = objectPayload(payload);
        service.rematch(socket, { gameId: requiredString(body, 'gameId', 64) });
      });
    });

    socket.on('disconnect', () => {
      safe(socket, () => service.handleDisconnect(socket));
    });
  });
}
