import { io, type Socket } from 'socket.io-client';
import {
  ClientEvents,
  type AttackPayload,
  type JoinRoomPayload,
  type PlayCardPayload,
  type ReconnectPayload,
} from '@coincard/shared';

// Wrapper mỏng quanh socket.io-client — component không gọi io() trực tiếp.
class SocketService {
  private socket: Socket | null = null;

  // Tái dùng socket đang có (kể cả đang connecting) — tránh StrictMode
  // double-mount tạo 2 connection/tab. Muốn socket mới thì disconnect() trước.
  connect(sessionToken?: string): Socket {
    if (this.socket) {
      return this.socket;
    }
    this.socket = io({
      auth: sessionToken ? { sessionToken } : {},
    });
    // Tự join lại phòng mỗi khi (re)connect — rớt mạng/server restart xong
    // socket tự nối lại thì game tiếp tục, không kẹt.
    this.socket.on('connect', () => {
      const auth = this.socket?.auth as { sessionToken?: string } | undefined;
      if (auth?.sessionToken) {
        this.socket?.emit(ClientEvents.RECONNECT_GAME, { sessionToken: auth.sessionToken });
      }
    });
    return this.socket;
  }

  getSocket(): Socket {
    if (!this.socket) {
      throw new Error('Socket chưa connect.');
    }
    return this.socket;
  }

  isConnected(): boolean {
    return this.socket?.connected ?? false;
  }

  // Server đọc sessionToken từ handshake.auth — gắn token rồi connect lại.
  // Sự kiện 'connect' (đăng ký trong connect()) sẽ tự gửi RECONNECT_GAME.
  // Listeners đã đăng ký được giữ nguyên khi dùng disconnect().connect().
  setSessionToken(token: string): void {
    if (!this.socket) {
      return;
    }
    this.socket.auth = { sessionToken: token };
    if (this.socket.connected) {
      this.socket.disconnect().connect();
    }
  }

  createRoom(playerName: string): void {
    this.getSocket().emit(ClientEvents.CREATE_ROOM, { playerName });
  }

  joinRoom(payload: JoinRoomPayload): void {
    this.getSocket().emit(ClientEvents.JOIN_ROOM, payload);
  }

  selectDeck(heroId: string, roomCode: string): void {
    this.getSocket().emit(ClientEvents.SELECT_DECK, { heroId, roomCode });
  }

  playCard(payload: PlayCardPayload): void {
    this.getSocket().emit(ClientEvents.PLAY_CARD, payload);
  }

  attack(payload: AttackPayload): void {
    this.getSocket().emit(ClientEvents.ATTACK, payload);
  }

  endTurn(gameId: string): void {
    this.getSocket().emit(ClientEvents.END_TURN, { gameId });
  }

  useHeroPower(gameId: string): void {
    this.getSocket().emit(ClientEvents.USE_HERO_POWER, { gameId });
  }

  concede(gameId: string): void {
    this.getSocket().emit(ClientEvents.CONCEDE, { gameId });
  }

  reconnectGame(payload: ReconnectPayload): void {
    this.getSocket().emit(ClientEvents.RECONNECT_GAME, payload);
  }

  disconnect(): void {
    this.socket?.disconnect();
    this.socket = null;
  }
}

export const socketService = new SocketService();
