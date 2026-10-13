import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from 'playwright-core';

const scenes = process.argv.slice(2);
if (scenes.length === 0 || scenes.some((scene) => !scene.endsWith('.sketch.js'))) {
  console.error('Usage: sketch <name>.sketch.js…');
  process.exit(2);
}

async function launch() {
  const options = [];
  if (process.env.CHROME_PATH) options.push({ executablePath: process.env.CHROME_PATH });
  options.push({ channel: 'chrome' }, {});
  if (existsSync('/opt/pw-browsers/chromium')) options.push({ executablePath: '/opt/pw-browsers/chromium' });
  const failures = [];
  for (const option of options) {
    try {
      return await chromium.launch(option);
    } catch (e) {
      failures.push(e.message.split('\n')[0]);
    }
  }
  throw new Error(`No browser found. Set CHROME_PATH to a Chrome or Chromium binary.\n${failures.join('\n')}`);
}

const pageUrl = pathToFileURL(path.join(path.dirname(fileURLToPath(import.meta.url)), 'page.html')).href;
const browser = await launch();
const page = await browser.newPage({ deviceScaleFactor: 2, viewport: { width: 2000, height: 1600 } });
let failed = false;
for (const scene of scenes) {
  const file = path.resolve(scene);
  const source = await readFile(file, 'utf8');
  await page.goto(pageUrl);
  await page.evaluate(() => window.fontsLoaded);
  try {
    await page.evaluate(
      ([source, name]) => {
        (0, eval)(`(function () {${source}\n})();\n//# sourceURL=${name}`);
        fit();
      },
      [source, path.basename(file)],
    );
  } catch (e) {
    console.error(`${scene}: ${e.message.replace(/^page\.evaluate: /, '').split('\n').slice(0, 2).join('\n')}`);
    failed = true;
    continue;
  }
  const out = file.replace(/\.sketch\.js$/, '.png');
  await page.locator('svg').screenshot({ path: out });
  console.log(path.relative(process.cwd(), out));
}
await browser.close();
process.exit(failed ? 1 : 0);
