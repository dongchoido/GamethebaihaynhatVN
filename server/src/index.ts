import express from 'express';
import cors from 'cors';
import { createServer } from 'node:http';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import 'dotenv/config';
import { Server } from 'socket.io';
import { registerSocketHandler } from './network/SocketHandler.js';

const clientDistPath = join(dirname(fileURLToPath(import.meta.url)), '../../client/dist');
if (!existsSync(join(clientDistPath, 'index.html'))) {
  throw new Error(`Không tìm thấy client build tại ${clientDistPath}. Hãy chạy "npm run build" trước khi start server.`);
}

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());
app.use(express.static(clientDistPath, { index: false }));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'coincard-server' });
});

app.get('*', (_req, res) => {
  res.sendFile(join(clientDistPath, 'index.html'));
});

const HOST = process.env.HOST ?? '127.0.0.1';
const PORT = Number(process.env.PORT ?? 3000);
if (!process.env.DATABASE_URL) {
  throw new Error('Thiếu DATABASE_URL. Hãy tạo server/.env từ server/.env.example trước khi start server.');
}
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
});

registerSocketHandler(io);

httpServer.listen(PORT, HOST, () => {
  console.log(`CoinCard server chạy trên http://${HOST}:${PORT}`);
});
