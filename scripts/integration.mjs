import { spawn } from 'node:child_process';
import { mkdtemp, readFile, writeFile } from 'node:fs/promises';
import { join, resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { PrismaClient } from '@prisma/client';
import { checkOnline } from './online-check.mjs';

const root = fileURLToPath(new URL('..', import.meta.url));
const directory = await mkdtemp(join(root, 'server/data/.integration-'));
const databaseUrl = `file:../data/${basename(directory)}/test.db`;
await writeFile(join(directory, 'test.db'), '');
const env = { ...process.env, DATABASE_URL: databaseUrl, HOST: '127.0.0.1', PORT: '0' };
async function run(args) {
  const child = spawn(process.execPath, args, { cwd: root, env, stdio: 'inherit' });
  await new Promise((resolve, reject) => {
    child.once('error', reject); child.once('exit', code => code === 0 ? resolve() : reject(new Error(`Exit ${code}`)));
  });
}
console.log(`Isolated test database: ${directory}`);
await run(['node_modules/prisma/build/index.js', 'migrate', 'deploy', '--schema', 'server/prisma/schema.prisma']);
await run(['node_modules/tsx/dist/cli.mjs', 'server/src/database/seed.ts']);
const server = spawn(process.execPath, ['server/dist/index.js', '--serve-client'], { cwd: root, env, stdio: ['ignore', 'pipe', 'inherit'] });
const db = new PrismaClient({ datasourceUrl: databaseUrl });
try {
  const url = await new Promise((resolve, reject) => {
    let output = ''; const timer = setTimeout(() => reject(new Error('Server startup timeout')), 15000);
    server.once('error', reject); server.once('exit', code => { clearTimeout(timer); reject(new Error(`Server exited ${code}`)); });
    server.stdout.on('data', chunk => {
      output += chunk;
      const match = output.match(/http:\/\/127\.0\.0\.1:\d+/);
      if (match) { clearTimeout(timer); resolve(match[0]); }
    });
  });
  console.log(`Test server: ${url}`);
  const page = await fetch(url); assert.equal(page.status, 200); const html = await page.text();
  for (const match of html.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g)) assert.equal((await fetch(url + match[1])).status, 200);
  const catalog = JSON.parse(await readFile(resolve(root, 'data/cards.json'), 'utf8'));
  for (const card of catalog) assert.equal((await fetch(`${url}/${card.imagePath}`)).status, 200, card.imagePath);
  for (const path of ['/.env', '/server/.env', '/src/index.ts', '/package.json']) assert.equal((await fetch(url + path)).status, 404, path);
  console.log('PASS production HTML, bundles, catalog images, source/config isolation');
  const ids = await checkOnline(url);
  for (let attempt = 0; attempt < 50; attempt++) {
    if (await db.game.count({ where: { id: { in: ids } } }) === 2) break;
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  assert.equal(await db.game.count({ where: { id: { in: ids } } }), 2);
  assert.equal(await db.gamePlayer.count({ where: { gameId: { in: ids } } }), 4);
  console.log('PASS database: two completed games, four participant records');
  if (process.argv.includes('--keep')) {
    console.log(`Browser/tunnel QA available at ${url}; Ctrl+C to stop.`);
    await new Promise(resolve => process.once('SIGINT', resolve));
  }
} finally { server.kill(); await db.$disconnect(); }
