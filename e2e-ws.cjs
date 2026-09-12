// Shim WebSocket cho E2E: cùng API tối thiểu của socket.io-client mà script cần
// (on/off/once/emit/disconnect/connect/reconnect + event 'connect'),
// nhưng nói envelope {event, data} với server Java.
function connect(url, opts = {}) {
  const target = url.replace(/^http/, 'ws') + '/ws';
  const listeners = new Map();
  const queue = [];
  const sock = {
    connected: false,
    _ws: null,
    _manual: false,
    on(event, handler) {
      if (!listeners.has(event)) listeners.set(event, new Set());
      listeners.get(event).add(handler);
    },
    off(event, handler) {
      listeners.get(event)?.delete(handler);
    },
    once(event, handler) {
      const wrapper = (data) => {
        sock.off(event, wrapper);
        handler(data);
      };
      sock.on(event, wrapper);
    },
    emit(event, data) {
      const text = JSON.stringify({ event, data: data ?? {} });
      if (sock._ws && sock._ws.readyState === 1) sock._ws.send(text);
      else queue.push(text);
    },
    connect() {
      sock._manual = false;
      open();
    },
    reconnect() {
      return new Promise((resolve) => {
        const done = () => {
          sock.off('connect', done);
          resolve();
        };
        sock.on('connect', done);
        try {
          sock._ws?.close();
        } catch {
          // Ignore.
        }
        sock._manual = false;
        open();
      });
    },
    disconnect() {
      sock._manual = true;
      queue.length = 0;
      try {
        sock._ws?.close();
      } catch {
        // Ignore.
      }
      sock._ws = null;
      sock.connected = false;
    },
  };
  function fire(event, data) {
    for (const handler of [...(listeners.get(event) ?? [])]) {
      try {
        handler(data);
      } catch {
        // Ignore.
      }
    }
  }
  function open() {
    const ws = new WebSocket(target);
    sock._ws = ws;
    ws.onopen = () => {
      sock.connected = true;
      while (queue.length > 0 && sock._ws === ws) {
        try {
          ws.send(queue.shift());
        } catch {
          break;
        }
      }
      fire('connect');
    };
    ws.onmessage = (msg) => {
      try {
        const parsed = JSON.parse(String(msg.data));
        if (parsed && typeof parsed.event === 'string') fire(parsed.event, parsed.data);
      } catch {
        // Ignore.
      }
    };
    ws.onerror = () => fire('connect_error');
    ws.onclose = () => {
      const was = sock.connected;
      sock.connected = false;
      if (sock._ws !== ws) return;
      sock._ws = null;
      if (was) fire('disconnect');
    };
  }
  if (opts.autoConnect !== false) open();
  return sock;
}

module.exports = { connect };
