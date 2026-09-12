// CompatSocket: WebSocket thuần nói cùng envelope {event, data} với server Java,
// nhưng giữ nguyên mặt API mà app đang dùng (on/off/emit/auth/connected/disconnect
// + event 'connect'/'disconnect'/'connect_error'). Thay thế socket.io-client.

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Handler = (data?: any) => void;

function wsUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${proto}//${window.location.host}/ws`;
}

export class CompatSocket {
  auth: { sessionToken?: string } = {};
  connected = false;

  private ws: WebSocket | null = null;
  private listeners = new Map<string, Set<Handler>>();
  private queue: string[] = [];
  private manualClose = false;
  private retryMs = 500;

  constructor(auth?: { sessionToken?: string }) {
    if (auth) this.auth = auth;
    this.open();
  }

  on(event: string, handler: Handler): void {
    let set = this.listeners.get(event);
    if (!set) {
      set = new Set();
      this.listeners.set(event, set);
    }
    set.add(handler);
  }

  off(event: string, handler: Handler): void {
    this.listeners.get(event)?.delete(handler);
  }

  emit(event: string, data?: unknown): void {
    const text = JSON.stringify({ event, data: data ?? {} });
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
        if (typeof parsed.event === 'string') this.fire(parsed.event, parsed.data);
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

  private fire(event: string, data?: unknown): void {
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
