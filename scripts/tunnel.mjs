import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
const local = fileURLToPath(new URL('../.tools/cloudflared.exe', import.meta.url));
const bin = process.platform === 'win32' && existsSync(local) ? local : 'cloudflared';
const child = spawn(bin, ['tunnel', '--url', 'http://127.0.0.1:3000', ...process.argv.slice(2)], { stdio: 'inherit' });
child.on('error', () => {
  console.error('Chưa có cloudflared. Cài từ https://developers.cloudflare.com/tunnel/downloads/ hoặc đặt file .tools/cloudflared.exe trên Windows. Chạy local vẫn hoạt động bình thường mà không cần tunnel.');
  process.exitCode = 1;
});
child.on('exit', code => { process.exitCode = code ?? 1; });
