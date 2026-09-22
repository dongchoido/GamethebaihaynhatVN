import {
  ClientEvents,
  ServerEvents,
  type ClientEventPayload,
  type ServerEventPayload,
} from '@coincard/shared';

// WebSocket client dùng envelope {event, data} và phát event trạng thái kết nối.
type LifecycleEvent = 'connect' | 'disconnect' | 'connect_error';
type SocketEvent = ClientEvents | ServerEvents | LifecycleEvent;

type EventPayload<Event extends SocketEvent> =
  Event extends ClientEvents ? ClientEventPayload<Event> :
  Event extends ServerEvents ? ServerEventPayload<Event> :
  undefined;

type Handler<Data> = (data: Data) => void;

function isServerEvent(event: string): event is ServerEvents {
  return Object.values(ServerEvents).includes(event as ServerEvents);
}

function wsUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws`;
}

export class CompatSocket {
  auth: { sessionToken?: string } = {};
  connected = false;

  private ws: WebSocket | null = null;
  private listeners = new Map<SocketEvent, Set<Handler<unknown>>>();
  private queue: string[] = [];
  private manualClose = false;
  private retryMs = 500;

  constructor(auth?: { sessionToken?: string }) {
    if (auth) this.auth = auth;
    this.open();
  }

  on<Event extends SocketEvent>(event: Event, handler: Handler<EventPayload<Event>>): void {
    let set = this.listeners.get(event);
    if (!set) {
      set = new Set();
      this.listeners.set(event, set);
    }
    set.add(handler as unknown as Handler<unknown>);
  }

  off<Event extends SocketEvent>(event: Event, handler: Handler<EventPayload<Event>>): void {
    this.listeners.get(event)?.delete(handler as unknown as Handler<unknown>);
  }

  emit<Event extends ClientEvents>(event: Event, data: ClientEventPayload<Event>): void {
    const text = JSON.stringify({ event, data });
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(text);
    } else {
      this.queue.push(text);
    }
  }

  disconnect(): void {
    this.manualClose = true;
    this.queue = [];
    try {
      this.ws?.close();
    } catch {
      // Ignore.
    }
    this.ws = null;
    this.setConnected(false);
  }

  private open(): void {
    this.manualClose = false;
    let ws: WebSocket;
    try {
      ws = new WebSocket(wsUrl());
    } catch {
      this.fire('connect_error');
      this.scheduleRetry();
      return;
    }
    this.ws = ws;
    ws.onopen = () => {
      this.retryMs = 500;
      this.setConnected(true);
      for (const text of this.queue.splice(0)) {
        try {
          ws.send(text);
        } catch {
          this.queue.unshift(text);
          break;
        }
      }
      this.fire('connect');
    };
    ws.onmessage = (msg) => {
      try {
        const parsed = JSON.parse(String(msg.data)) as { event?: string; data?: unknown };
        if (typeof parsed.event === 'string' && isServerEvent(parsed.event)) {
          this.fire(parsed.event, parsed.data as ServerEventPayload<typeof parsed.event>);
        }
      } catch {
        // Bỏ qua frame lỗi — không sập app.
      }
    };
    ws.onerror = () => this.fire('connect_error');
    ws.onclose = () => {
      const wasConnected = this.connected;
      this.setConnected(false);
      if (!this.manualClose) {
        if (wasConnected) this.fire('disconnect');
        this.scheduleRetry();
      }
    };
  }

  private scheduleRetry(): void {
    if (this.manualClose) return;
    const delay = this.retryMs;
    this.retryMs = Math.min(this.retryMs * 2, 5000);
    window.setTimeout(() => {
      if (!this.manualClose && !this.connected) this.open();
    }, delay);
  }

  private setConnected(value: boolean): void {
    this.connected = value;
  }

  private fire<Event extends SocketEvent>(event: Event, data?: EventPayload<Event>): void {
    const set = this.listeners.get(event);
    if (!set) return;
    for (const handler of [...set]) {
      try {
        handler(data);
      } catch {
        // Handler lỗi không được giết socket.
      }
    }
  }
}
