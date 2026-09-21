import { spawn, spawnSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const jar = fileURLToPath(
  new URL('../server-java/target/coincard-server-0.0.1.jar', import.meta.url),
);

function reservePort() {
  return new Promise((resolve, reject) => {
    const socket = createServer();
    socket.once('error', reject);
    socket.listen(0, '127.0.0.1', () => {
      const address = socket.address();
      const port = typeof address === 'object' && address ? address.port : 0;
      socket.close((error) => error ? reject(error) : resolve(port));
    });
  });
}

async function waitForServer(server, gameUrl) {
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (server.exitCode !== null) {
      throw new Error(`Spring Boot dừng sớm với exit code ${server.exitCode}.`);
    }
    try {
      const response = await fetch(`${gameUrl}/health`);
      if (response.ok) return;
    } catch {
      // Server đang khởi động.
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Spring Boot không sẵn sàng sau 30 giây.');
}

function run(script, gameUrl) {
  const result = spawnSync(process.execPath, [script], {
    cwd: root,
    stdio: 'inherit',
    env: { ...process.env, GAME_URL: gameUrl },
  });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${script} thất bại.`);
}

const port = await reservePort();
const gameUrl = `http://127.0.0.1:${port}`;
const tempDir = mkdtempSync(join(tmpdir(), 'coincard-verify-'));
let output = '';
const server = spawn('java', ['-jar', jar], {
  cwd: root,
  stdio: ['ignore', 'pipe', 'pipe'],
  env: {
    ...process.env,
    PORT: String(port),
    DATABASE_URL: '',
    COINCARD_DB_PATH: join(tempDir, 'verify.db'),
  },
});
server.stdout.on('data', (chunk) => { output += chunk; });
server.stderr.on('data', (chunk) => { output += chunk; });

try {
  await waitForServer(server, gameUrl);
  const rootResponse = await fetch(`${gameUrl}/`);
  if (!rootResponse.ok) throw new Error(`Trang production trả HTTP ${rootResponse.status}.`);
  run('e2e-two-players.cjs', gameUrl);
  run('e2e-negative.cjs', gameUrl);
  run('e2e-connection.cjs', gameUrl);
} catch (error) {
  if (output) console.error(output);
  throw error;
} finally {
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/PID', String(server.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    server.kill('SIGTERM');
  }
  rmSync(tempDir, { recursive: true, force: true });
}
