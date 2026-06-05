interface Marketing {
  featureGraphic: { headline: string; tagline: string }
  screenshots: { caption: string }[]
}

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

async function ready(): Promise<void> {
  const params = new URLSearchParams(location.search)
  const locale = params.get('locale') ?? 'en-US'
  const index = parseInt(params.get('index') ?? '0', 10)
  const asset = document.body.dataset.asset
  document.documentElement.lang = locale.split('-')[0] ?? locale

  const m = await fetchMarketing(locale)

  if (asset === 'feature-graphic') {
    setText('.headline', m.featureGraphic.headline)
    setText('.tagline', m.featureGraphic.tagline)
    setSrc('.phone-frame img', await pickRaw('phone', 0))
  } else if (asset === 'phone' || asset === 'tablet-7' || asset === 'tablet-10') {
    const caption = m.screenshots[index]?.caption
    if (!caption) throw new Error(`No caption ${index + 1} found for ${locale}`)
    setText('.caption', caption)
    setSrc('.device img', await pickRaw(asset, index))
  }

  await Promise.all(
    Array.from(document.images).map((img) =>
      img.complete ? Promise.resolve() : new Promise((r) => img.addEventListener('load', r, { once: true })),
    ),
  )
  document.body.dataset.ready = '1'
}

ready().catch((err) => {
  document.body.textContent = String(err)
  document.body.dataset.ready = 'error'
})
