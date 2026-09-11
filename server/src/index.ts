import express from 'express';
import cors from 'cors';
import { createServer } from 'node:http';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { HOST, PORT, SERVE_CLIENT } from './config.js';
import { prisma } from './database/prismaClient.js';
import { Server } from 'socket.io';
import { registerSocketHandler } from './network/SocketHandler.js';

const clientDistPath = join(dirname(fileURLToPath(import.meta.url)), '../../client/dist');
if (SERVE_CLIENT && !existsSync(join(clientDistPath, 'index.html'))) {
  throw new Error(`Không tìm thấy client build tại ${clientDistPath}. Hãy chạy "npm run build" trước khi start server.`);
}

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'coincard-server' });
});

if (SERVE_CLIENT) {
  app.use(express.static(clientDistPath, { index: false, dotfiles: 'deny' }));
  app.get('/', (_req, res) => {
    res.setHeader('Cache-Control', 'no-cache');
    res.sendFile(join(clientDistPath, 'index.html'));
  });
}
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
});

registerSocketHandler(io);

httpServer.on('error', error => {
  console.error('Không mở được server. Kiểm tra HOST/PORT hoặc cổng đang được dùng:', error.message);
  void prisma.$disconnect(); process.exitCode = 1;
});

async function start(): Promise<void> {
  if (SERVE_CLIENT) {
    const [cards, heroes] = await Promise.all([prisma.card.count(), prisma.hero.count()]);
    if (cards < 30 || heroes < 5) throw new Error('Database chưa có đủ catalog. Chạy migrate và seed khi cài đặt lần đầu.');
  }
  httpServer.listen(PORT, HOST, () => {
    const address = httpServer.address();
    console.log(`CoinCard server chạy trên http://${HOST}:${typeof address === 'object' && address ? address.port : PORT}`);
  });
}
void start().catch(error => {
  console.error('Không khởi động được CoinCard; kiểm tra database/migration:', error.message);
  void prisma.$disconnect(); process.exitCode = 1;
});
for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.once(signal, () => { io.close(() => { void prisma.$disconnect().then(() => process.exit(0)); }); });
}
