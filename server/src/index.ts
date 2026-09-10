import express from 'express';
import cors from 'cors';
import { createServer } from 'node:http';
import { Server } from 'socket.io';
import { registerSocketHandler } from './network/SocketHandler.js';

const app = express();
app.use(cors());
app.use(express.json());

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

const PORT = Number(process.env.PORT ?? 3000);
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
});

registerSocketHandler(io);

httpServer.listen(PORT, () => {
  console.log(`CoinCard server chạy trên http://localhost:${PORT}`);
});
