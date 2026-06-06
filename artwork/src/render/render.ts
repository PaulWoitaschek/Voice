import { mkdirSync, existsSync, rmSync } from 'node:fs'
import { join, resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium, type Browser, type Page } from 'playwright'
import sharp from 'sharp'
import { createServer, type ViteDevServer } from 'vite'
import { listLocaleDirs, listLocales, imagesDir } from './locales.ts'
import { readMarketing } from './strings.ts'
import os from 'node:os';

type AssetKind = 'feature-graphic' | 'phone' | 'tablet-7' | 'tablet-10'

interface AssetSpec {
  kind: AssetKind
  template: string
  width: number
  height: number
  outDir: (locale: string) => string
  outName: (locale: string, index: number) => string
  outFormat: 'png' | 'jpeg'
}

const ASSETS: AssetSpec[] = [
  {
    kind: 'feature-graphic',
    template: '/templates/feature-graphic.html',
    width: 1024,
    height: 500,
    outDir: (locale) => imagesDir(locale),
    outName: () => 'featureGraphic.png',
    outFormat: 'png',
  },
  {
    kind: 'phone',
    template: '/templates/phone-screenshot.html',
    width: 1080,
    height: 2340,
    outDir: (locale) => join(imagesDir(locale), 'phoneScreenshots'),
    outName: (locale, i) => `${i + 1}_${locale}.png`,
    outFormat: 'png',
  }
]

const CONCURRENCY = os.availableParallelism();
const PNG_OPTIONS = {
  adaptiveFiltering: true,
  compressionLevel: 9,
} as const
const GENERATED_IMAGE_FILES = ['featureGraphic.jpeg', 'featureGraphic.jpg', 'featureGraphic.png']
const GENERATED_SCREENSHOT_DIRS = [
  'phoneScreenshots',
  'sevenInchScreenshots',
  'tenInchScreenshots',
  'tvScreenshots',
  'wearScreenshots',
]

async function renderOne(
  browser: Browser,
  baseUrl: string,
  asset: AssetSpec,
  locale: string,
  index: number,
): Promise<string> {
  const url = `${baseUrl}${asset.template}?locale=${encodeURIComponent(locale)}&index=${index}`
  // Render at 3× then downsample with Lanczos3. The browser rasterises text and
  // image scaling with more pixel data, so the final 1× PNG (esp. the small
  // phone-mockup inside the feature graphic) reads as crisp rather than fuzzy.
  // 3× over 2× makes a visible difference on the 1024×500 feature graphic
  // because Play Store displays it at 2-3× DPR on retina screens.
  const SUPERSAMPLE = 3
  const page: Page = await browser.newPage({
    viewport: { width: asset.width, height: asset.height },
    deviceScaleFactor: SUPERSAMPLE,
  })
  try {
    await page.goto(url, { waitUntil: 'load' })
    await page.waitForFunction(() => document.body.dataset.ready === '1', undefined, { timeout: 30000 })
    const dir = asset.outDir(locale)
    mkdirSync(dir, { recursive: true })
    const out = join(dir, asset.outName(locale, index))
    const pngBuffer = await page.locator('#canvas').screenshot({ type: 'png' })
    const resized = sharp(pngBuffer).resize(asset.width, asset.height, { kernel: 'lanczos3' })
    if (asset.outFormat === 'jpeg') {
      await resized.jpeg({ quality: 92 }).toFile(out)
    } else {
      await resized.png(PNG_OPTIONS).toFile(out)
    }
    return out
  } finally {
    await page.close()
  }
}

interface Job {
  asset: AssetSpec
  locale: string
  index: number
}

function assetCount(asset: AssetSpec, locale: string): number {
  if (asset.kind === 'feature-graphic') return 1
  return readMarketing(locale).screenshots.length
}

async function runPool<T>(jobs: T[], concurrency: number, worker: (j: T) => Promise<void>): Promise<void> {
  let cursor = 0
  const runners = Array.from({ length: Math.min(concurrency, jobs.length) }, async () => {
    while (true) {
      const i = cursor++
      if (i >= jobs.length) return
      await worker(jobs[i])
    }
  })
  await Promise.all(runners)
}

function cleanGeneratedImages(locale: string): void {
  const dir = imagesDir(locale)
  if (!existsSync(dir)) return

  for (const file of GENERATED_IMAGE_FILES) {
    rmSync(join(dir, file), { force: true })
  }
  for (const screenshotDir of GENERATED_SCREENSHOT_DIRS) {
    rmSync(join(dir, screenshotDir), { recursive: true, force: true })
  }
}

async function main() {
  const locales = listLocales()
  if (locales.length === 0) {
    console.error('No locales with marketing.yml found.')
    process.exit(1)
  }
  console.log(`Rendering for ${locales.length} locales: ${locales.join(', ')} with concurrency: ${CONCURRENCY}`)

  for (const locale of listLocaleDirs()) {
    cleanGeneratedImages(locale)
  }

  const HERE = dirname(fileURLToPath(import.meta.url))
  const server: ViteDevServer = await createServer({
    configFile: resolve(HERE, '../../vite.config.ts'),
    server: { port: 5173, strictPort: true },
    logLevel: 'warn',
  })
  await server.listen()
  const baseUrl = `http://localhost:${server.config.server.port}`

  const browser = await chromium.launch()

  const jobs: Job[] = []
  for (const locale of locales) {
    for (const asset of ASSETS) {
      for (let i = 0; i < assetCount(asset, locale); i++) {
        jobs.push({ asset, locale, index: i })
      }
    }
  }

  let done = 0
  await runPool(jobs, CONCURRENCY, async ({ asset, locale, index }) => {
    const out = await renderOne(browser, baseUrl, asset, locale, index)
    done++
    console.log(`[${done}/${jobs.length}] ${out}`)
  })

  await browser.close()
  await server.close()
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
