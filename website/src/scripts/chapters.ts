export type Chapter = { startMs: number; title: string }

export function id3Chapters(frames: unknown, id3Version: number): Chapter[] {
  const list = frames instanceof Uint8Array ? [frames] : Array.isArray(frames) ? frames : []
  const chapters = list
    .filter((frame): frame is Uint8Array => frame instanceof Uint8Array)
    .map((frame) => chapFrame(frame, id3Version))
    .filter((chapter) => chapter !== null)
    .sort((a, b) => a.startMs - b.startMs)
  return chapters.map((chapter, index) => ({ ...chapter, title: chapter.title || `${index + 1}` }))
}

function chapFrame(frame: Uint8Array, id3Version: number): Chapter | null {
  const view = new DataView(frame.buffer, frame.byteOffset, frame.byteLength)
  let pos = frame.indexOf(0) + 1
  if (pos === 0 || pos + 16 > frame.length) return null
  const startMs = view.getUint32(pos)
  pos += 16
  let title = ''
  while (pos + 10 <= frame.length) {
    const id = String.fromCharCode(...frame.subarray(pos, pos + 4))
    const size = id3Version === 4 ? synchsafe(view.getUint32(pos + 4)) : view.getUint32(pos + 4)
    pos += 10
    if (id === 'TIT2') title = id3Text(frame.subarray(pos, Math.min(pos + size, frame.length)))
    pos += size
  }
  return { startMs, title }
}

function synchsafe(value: number) {
  return (value & 0x7f) | ((value >> 8) & 0x7f) << 7 | ((value >> 16) & 0x7f) << 14 | ((value >> 24) & 0x7f) << 21
}

function id3Text(body: Uint8Array) {
  const text = body.subarray(1)
  let encoding = ['latin1', 'utf-16le', 'utf-16be', 'utf-8'][body[0]] ?? 'utf-8'
  if (body[0] === 1 && text[0] === 0xfe && text[1] === 0xff) encoding = 'utf-16be'
  return new TextDecoder(encoding).decode(text).replace(/\0.*$/s, '').trim()
}

// Nero chapters (moov/udta/chpl), which Voice and most players read. The count is one byte.
export function chplBox(chapters: Chapter[]): Uint8Array {
  const encoder = new TextEncoder()
  const titles = chapters.slice(0, 255).map((chapter) => {
    const title = new Uint8Array(255)
    return title.subarray(0, encoder.encodeInto(chapter.title, title).written)
  })
  const size = 17 + titles.reduce((sum, title) => sum + 9 + title.length, 0)
  const box = new Uint8Array(size)
  const view = new DataView(box.buffer)
  view.setUint32(0, size)
  box.set(encoder.encode('chpl'), 4)
  box[8] = 1
  box[16] = titles.length
  let pos = 17
  titles.forEach((title, index) => {
    view.setBigUint64(pos, BigInt(chapters[index].startMs) * 10_000n)
    box[pos + 8] = title.length
    box.set(title, pos + 9)
    pos += 9 + title.length
  })
  return box
}

// The moov box comes after the audio data, so making it larger moves no audio offsets.
export function withBox(moov: Uint8Array, child: Uint8Array): Uint8Array {
  const view = new DataView(moov.buffer, moov.byteOffset, moov.byteLength)
  let pos = 8
  while (pos + 8 <= moov.length) {
    const size = view.getUint32(pos)
    if (size < 8) break
    if (boxType(moov, pos) === 'udta') {
      return resized(concat(moov.subarray(0, pos), resized(concat(moov.subarray(pos, pos + size), child)), moov.subarray(pos + size)))
    }
    pos += size
  }
  const udta = new Uint8Array(8)
  udta.set(new TextEncoder().encode('udta'), 4)
  return resized(concat(moov, resized(concat(udta, child))))
}

function boxType(bytes: Uint8Array, pos: number) {
  return String.fromCharCode(...bytes.subarray(pos + 4, pos + 8))
}

function resized(box: Uint8Array) {
  new DataView(box.buffer, box.byteOffset).setUint32(0, box.length)
  return box
}

function concat(...parts: Uint8Array[]) {
  const result = new Uint8Array(parts.reduce((sum, part) => sum + part.length, 0))
  let pos = 0
  for (const part of parts) {
    result.set(part, pos)
    pos += part.length
  }
  return result
}
