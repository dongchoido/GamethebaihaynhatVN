import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const serverDir = fileURLToPath(new URL('../server-java/', import.meta.url));
const cardsPath = fileURLToPath(new URL('../data/cards.json', import.meta.url));
const databasePath = fileURLToPath(new URL('../data/coincard.db', import.meta.url));
const clientDistPath = fileURLToPath(new URL('../client/dist/', import.meta.url));
const wrapper = fileURLToPath(new URL('../server-java/mvnw', import.meta.url));
const windowsWrapper = fileURLToPath(new URL('../server-java/mvnw.cmd', import.meta.url));
const mavenArgs = ['-f', `${serverDir}pom.xml`, ...process.argv.slice(2)];
const isWindows = process.platform === 'win32';
function detectJavaHome() {
  if (process.env.JAVA_HOME) return process.env.JAVA_HOME;
  const probe = spawnSync('java', ['-XshowSettings:properties', '-version'], {
    encoding: 'utf8',
    windowsHide: true,
  });
  const output = `${probe.stdout ?? ''}\n${probe.stderr ?? ''}`;
  const match = output.match(/^\s*java\.home\s*=\s*(.+)$/m);
  return match?.[1]?.trim();
}
const windowsCommand = `"${windowsWrapper}" ${mavenArgs
  .map((arg) => `"${arg.replaceAll('"', '""')}"`)
  .join(' ')}`;
const options = {
  cwd: serverDir,
  stdio: 'inherit',
  // Maven runs from server-java; runtime assets remain rooted at the repository root.
  env: {
    ...process.env,
    JAVA_HOME: detectJavaHome() ?? process.env.JAVA_HOME,
    COINCARD_CARDS_PATH: process.env.COINCARD_CARDS_PATH || cardsPath,
    COINCARD_DB_PATH: process.env.COINCARD_DB_PATH || databasePath,
    COINCARD_CLIENT_DIST: process.env.COINCARD_CLIENT_DIST || clientDistPath,
  },
};
const result = isWindows
  ? spawnSync(windowsCommand, { ...options, shell: true })
  : spawnSync('sh', [wrapper, ...mavenArgs], options);

if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
