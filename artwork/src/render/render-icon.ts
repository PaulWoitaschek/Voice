import { readFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import sharp from 'sharp'
import { FASTLANE_DIR } from './locales.ts'

const HERE = dirname(fileURLToPath(import.meta.url))
const ARTWORK_DIR = resolve(HERE, '../..')
const LOGO_SVG = join(ARTWORK_DIR, 'public/logo.svg')
const PNG_OPTIONS = {
  compressionLevel: 9,
} as const

interface IconOutput {
  path: string
  size: number
  /** Scale applied via SVG wrapping so the adaptive-icon safe-zone padding is */
  /* removed. 1.0 = full 108dp viewport (with padding); 1.18 ≈ Play Store mask. */
  zoom: number
}

// zoom 1.30 ≈ Android Studio's launcher mask: crops the
// adaptive-icon safe-zone padding so waves fill the visible area instead of
// floating in a sea of blue. The Play Store renders icon.png as-is (no mask),
// so this is also how the user sees it on the storefront.
const OUTPUTS: IconOutput[] = [
  { path: join(FASTLANE_DIR, 'en-US/images/icon.png'), size: 512, zoom: 1.3 },
]

async function rasterize(svg: string, size: number, zoom: number): Promise<Buffer> {
  if (zoom === 1) {
    return sharp(Buffer.from(svg), { density: 384 }).resize(size, size).png(PNG_OPTIONS).toBuffer()
  }
  // Wrap the source SVG and scale its contents around the center so the inner
  // padding of the adaptive icon disappears — mimics the Play Store circle mask.
  const wrapped = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
    <g transform="translate(54 54) scale(${zoom}) translate(-54 -54)">
      ${svg.replace(/<\?xml[^>]*\?>/, '').replace(/<svg[^>]*>/, '').replace(/<\/svg>/, '')}
    </g>
  </svg>`
  return sharp(Buffer.from(wrapped), { density: 384 }).resize(size, size).png(PNG_OPTIONS).toBuffer()
}

async function main() {
  const svg = readFileSync(LOGO_SVG, 'utf8')
  for (const o of OUTPUTS) {
    mkdirSync(dirname(o.path), { recursive: true })
    const buf = await rasterize(svg, o.size, o.zoom)
    await sharp(buf).png(PNG_OPTIONS).toFile(o.path)
    console.log(`wrote ${o.path} (${o.size}×${o.size}, zoom ${o.zoom})`)
  }
}

main().catch((err) => { console.error(err); process.exit(1) })
