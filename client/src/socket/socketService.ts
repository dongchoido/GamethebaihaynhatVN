import {
  ClientEvents,
  type CreateRoomPayload,
  type GameActionPayload,
  type HeroClass,
  type AttackPayload,
  type JoinRoomPayload,
  type PlayCardPayload,
  type ReconnectGamePayload,
  type SubmitLoadoutPayload,
} from '@coincard/shared';
import { CompatSocket } from './compatSocket';

// Wrapper mỏng quanh CompatSocket để component không gọi transport trực tiếp.
class SocketService {
  private socket: CompatSocket | null = null;

  // Tái dùng socket đang có (kể cả đang connecting) — tránh StrictMode
  // double-mount tạo 2 connection/tab. Muốn socket mới thì disconnect() trước.
  connect(sessionToken?: string): CompatSocket {
    if (this.socket) {
      return this.socket;
    }
    this.socket = new CompatSocket(sessionToken ? { sessionToken } : {});
    // Tự join lại phòng mỗi khi (re)connect — rớt mạng/server restart xong
    // socket tự nối lại thì game tiếp tục, không kẹt.
    this.socket.on('connect', () => {
      const auth = this.socket?.auth as { sessionToken?: string } | undefined;
      if (auth?.sessionToken) {
        const payload: ReconnectGamePayload = { sessionToken: auth.sessionToken };
        this.socket?.emit(ClientEvents.RECONNECT_GAME, payload);
      }
    });
    return this.socket;
  }

  getSocket(): CompatSocket {
    if (!this.socket) {
      throw new Error('Socket chưa connect.');
    }
    return this.socket;
  }

  isConnected(): boolean {
    return this.socket?.connected ?? false;
  }

  // Server đã gắn session vào socket khi tạo/join phòng.
  // Chỉ lưu auth cho lần reconnect thật, không ngắt kết nối đang chơi.
  setSessionToken(token: string): void {
    if (!this.socket) {
      return;
    }
    this.socket.auth = { sessionToken: token };
  }

  createRoom(playerName: string): void {
    const payload: CreateRoomPayload = { playerName };
    this.getSocket().emit(ClientEvents.CREATE_ROOM, payload);
  }

  joinRoom(payload: JoinRoomPayload): void {
    this.getSocket().emit(ClientEvents.JOIN_ROOM, payload);
  }

  submitLoadout(heroClass: HeroClass, roomCode: string, cardSlugs: string[]): void {
    const payload: SubmitLoadoutPayload = { heroClass, roomCode, cardSlugs };
    this.getSocket().emit(ClientEvents.SUBMIT_LOADOUT, payload);
  }

  playCard(payload: PlayCardPayload): void {
    this.getSocket().emit(ClientEvents.PLAY_CARD, payload);
  }

  attack(payload: AttackPayload): void {
    this.getSocket().emit(ClientEvents.ATTACK, payload);
  }

  endTurn(gameId: string): void {
    const payload: GameActionPayload = { gameId };
    this.getSocket().emit(ClientEvents.END_TURN, payload);
  }

  useHeroPower(gameId: string): void {
    const payload: GameActionPayload = { gameId };
    this.getSocket().emit(ClientEvents.USE_HERO_POWER, payload);
  }


  concede(gameId: string): void {
    const payload: GameActionPayload = { gameId };
    this.getSocket().emit(ClientEvents.CONCEDE, payload);
  }

  clearSession(): void {
    if (this.socket) this.socket.auth = {};
  }

  rematch(gameId: string): void {
    const payload: GameActionPayload = { gameId };
    this.getSocket().emit(ClientEvents.REMATCH, payload);
  }

  disconnect(): void {
    this.socket?.disconnect();
    this.socket = null;
  }
}

export const socketService = new SocketService();
