import {
  ClientEvents,
  type AttackPayload,
  type JoinRoomPayload,
  type PlayCardPayload,
  type ReconnectPayload,
} from '@coincard/shared';
import { CompatSocket } from './compatSocket';

// Wrapper mỏng quanh CompatSocket (WebSocket thuần tới server Java) —
// component không gọi transport trực tiếp. Giữ nguyên API cũ của socket.io.
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
        this.socket?.emit(ClientEvents.RECONNECT_GAME, { sessionToken: auth.sessionToken });
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

  drawCard(gameId: string): void {
    this.getSocket().emit(ClientEvents.DRAW_CARD, { gameId });
  }

  concede(gameId: string): void {
    this.getSocket().emit(ClientEvents.CONCEDE, { gameId });
  }

  clearSession(): void {
    if (this.socket) this.socket.auth = {};
  }

  rematch(gameId: string): void {
    this.getSocket().emit(ClientEvents.REMATCH, { gameId });
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
