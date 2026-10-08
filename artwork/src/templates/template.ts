import { decorate, type Shape, type Star } from './decor.ts'

interface Marketing {
  featureGraphic: { headline: string; tagline: string }
  screenshots: { caption: string }[]
}

type Tone = 'day' | 'amber' | 'night' | 'sunset'

// One tone per raw phone screenshot, matching the colors on that screen.
const PHONE_TONES: Tone[] = ['day', 'amber', 'night', 'day', 'day', 'day', 'sunset', 'day']

async function fetchMarketing(locale: string): Promise<Marketing> {
  const res = await fetch(`/api/marketing/${encodeURIComponent(locale)}.json`)
  if (!res.ok) throw new Error(`marketing.yml not found for ${locale}`)
  return res.json()
}

async function fetchRawList(formFactor: string): Promise<string[]> {
  const res = await fetch(`/api/raw/${encodeURIComponent(formFactor)}.json`)
  if (!res.ok) return []
  return res.json()
}

async function pickRaw(formFactor: string, index: number): Promise<string> {
  const files = await fetchRawList(formFactor)
  const file = files[index]
  if (!file) throw new Error(`No raw .png screenshot ${index + 1} found in public/raw/${formFactor}/`)
  return `/raw/${formFactor}/${file}`
}

function setText(selector: string, value: string) {
  const el = document.querySelector(selector)
  if (el) el.textContent = value
}

function setSrc(selector: string, src: string) {
  const el = document.querySelector<HTMLImageElement>(selector)
  if (el) el.src = src
}

function shrinkToFit(el: HTMLElement, fits: () => boolean, minSize: number) {
  let size = parseFloat(getComputedStyle(el).fontSize)
  while (!fits() && size > minSize) {
    size -= 2
    el.style.fontSize = `${size}px`
  }
}

function overflows(el: HTMLElement): boolean {
  return el.scrollWidth > el.clientWidth + 1
}

function phoneDecor(index: number, tone: Tone): { shapes: Shape[]; stars: Star[] } {
  const mirrored = index % 2 === 1
  const x = (value: number) => (mirrored ? 1080 - value : value)
  const shapes: Shape[] = [
    { kind: mirrored ? 'flower' : 'cookie', x: x(990), y: 360, r: 300, rotate: index * 17 },
    { kind: mirrored ? 'burst' : 'clover', x: x(70), y: 2130, r: 280, rotate: index * 23, color: 2 },
    { kind: 'sunny', x: x(40), y: 1150, r: 120, rotate: index * 11, color: 2 },
  ]
  const stars: Star[] = [
    { x: x(120), y: 120, r: 26 },
    { x: x(1010), y: 980, r: 22 },
    { x: x(70), y: 1560, r: 16, opacity: 0.5 },
    { x: x(1020), y: 1700, r: 30 },
    { x: x(940), y: 2260, r: 14, opacity: 0.5 },
  ]
  if (tone === 'night') {
    for (let i = 0; i < 18; i++) {
      const left = i % 2 === 0
      stars.push({
        x: left ? 20 + ((i * 37) % 90) : 970 + ((i * 53) % 90),
        y: 80 + ((i * 433) % 2200),
        r: 5 + (i % 4) * 3,
        opacity: 0.35 + (i % 3) * 0.2,
      })
    }
  }
  return { shapes, stars }
}

const FEATURE_DECOR: { shapes: Shape[]; stars: Star[] } = {
  shapes: [
    { kind: 'cookie', x: 840, y: 250, r: 240, rotate: 10 },
    { kind: 'clover', x: -30, y: 520, r: 150, rotate: 20, color: 2 },
  ],
  stars: [
    { x: 600, y: 64, r: 16 },
    { x: 990, y: 440, r: 18 },
    { x: 560, y: 452, r: 10, opacity: 0.5 },
    { x: 1000, y: 60, r: 10, opacity: 0.5 },
  ],
}

async function ready(): Promise<void> {
  const params = new URLSearchParams(location.search)
  const locale = params.get('locale') ?? 'en-US'
  const index = parseInt(params.get('index') ?? '0', 10)
  const asset = document.body.dataset.asset
  document.documentElement.lang = locale.split('-')[0] ?? locale
  const canvas = document.getElementById('canvas')!
  const decor = document.querySelector('.decor')!

  const m = await fetchMarketing(locale)

  if (asset === 'feature-graphic') {
    setText('.headline', m.featureGraphic.headline)
    setText('.tagline', m.featureGraphic.tagline)
    setSrc('.phone-frame img', await pickRaw('feature', 0))
    decorate(decor, FEATURE_DECOR.shapes, FEATURE_DECOR.stars)
  } else if (asset === 'phone' || asset === 'tablet-7' || asset === 'tablet-10') {
    const caption = m.screenshots[index]?.caption
    if (!caption) throw new Error(`No caption ${index + 1} found for ${locale}`)
    setText('.caption', caption)
    setSrc('.device img', await pickRaw(asset, index))
    const tone = PHONE_TONES[index] ?? 'day'
    canvas.dataset.tone = tone
    if (asset === 'phone') {
      const { shapes, stars } = phoneDecor(index, tone)
      decorate(decor, shapes, stars)
    }
  }

  await document.fonts.ready
  await Promise.all(
    Array.from(document.images).map((img) =>
      img.complete ? Promise.resolve() : new Promise((r) => img.addEventListener('load', r, { once: true })),
    ),
  )

  if (asset === 'feature-graphic') {
    const left = document.querySelector<HTMLElement>('.left')!
    const headline = document.querySelector<HTMLElement>('.headline')!
    const tagline = document.querySelector<HTMLElement>('.tagline')!
    shrinkToFit(headline, () => left.offsetHeight <= 420 && !overflows(headline), 40)
    shrinkToFit(tagline, () => left.offsetHeight <= 420 && !overflows(tagline), 18)
  } else {
    const box = document.querySelector<HTMLElement>('.caption-box')!
    const caption = document.querySelector<HTMLElement>('.caption')!
    shrinkToFit(caption, () => caption.offsetHeight <= box.clientHeight && !overflows(caption), 56)
  }

  document.body.dataset.ready = '1'
}

ready().catch((err) => {
  document.body.textContent = String(err)
  document.body.dataset.ready = 'error'
})
