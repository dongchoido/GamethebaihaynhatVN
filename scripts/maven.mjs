import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const serverDir = fileURLToPath(new URL('../server-java/', import.meta.url));
const wrapper = fileURLToPath(new URL('../server-java/mvnw', import.meta.url));
const windowsWrapper = fileURLToPath(new URL('../server-java/mvnw.cmd', import.meta.url));
const mavenArgs = ['-f', `${serverDir}pom.xml`, ...process.argv.slice(2)];
const isWindows = process.platform === 'win32';
const windowsCommand = `"${windowsWrapper}" ${mavenArgs
  .map((arg) => `"${arg.replaceAll('"', '""')}"`)
  .join(' ')}`;
const options = {
  cwd: fileURLToPath(new URL('..', import.meta.url)),
  stdio: 'inherit',
};
const result = isWindows
  ? spawnSync(windowsCommand, { ...options, shell: true })
  : spawnSync('sh', [wrapper, ...mavenArgs], options);

if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
