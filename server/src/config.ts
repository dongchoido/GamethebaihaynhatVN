import { config } from 'dotenv';
import { fileURLToPath } from 'node:url';

// Resolve relative to this module in both src/ and dist/, never the shell cwd.
config({ path: fileURLToPath(new URL('../.env', import.meta.url)) });
export const PORT = Number(process.env.PORT ?? 3000);
export const HOST = process.env.HOST ?? '127.0.0.1';
export const SERVE_CLIENT = process.argv.includes('--serve-client');
if (!Number.isInteger(PORT) || PORT < 0 || PORT > 65535) throw new Error('PORT phải nằm trong 0..65535.');
if (!process.env.DATABASE_URL?.trim()) throw new Error('Thiếu DATABASE_URL. Tạo server/.env từ server/.env.example.');
