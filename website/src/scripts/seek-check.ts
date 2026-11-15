export type SeekCheck = { kind: 'not-mp3' } | { kind: 'mp3'; errorSeconds: number; variableBitrate: boolean; duration: number }

type Header = { mpeg1: boolean; sampleRate: number; bitrate: number; size: number; mono: boolean }

const BITRATES_MPEG1 = [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320]
const BITRATES_MPEG2 = [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160]
const SAMPLE_RATES = [44100, 48000, 32000]

function header(view: DataView, pos: number): Header | null {
  const h = view.getUint32(pos)
  if (h >>> 21 !== 0x7ff) return null
  const version = (h >>> 19) & 3
  const bitrateIndex = (h >>> 12) & 15
  const sampleRateIndex = (h >>> 10) & 3
  if (version === 1 || ((h >>> 17) & 3) !== 1 || bitrateIndex === 0 || bitrateIndex === 15 || sampleRateIndex === 3) {
    return null
  }
  const mpeg1 = version === 3
  const sampleRate = SAMPLE_RATES[sampleRateIndex] / (mpeg1 ? 1 : version === 2 ? 2 : 4)
  const bitrate = (mpeg1 ? BITRATES_MPEG1 : BITRATES_MPEG2)[bitrateIndex] * 1000
  const size = Math.floor(((mpeg1 ? 144 : 72) * bitrate) / sampleRate) + ((h >>> 9) & 1)
  return { mpeg1, sampleRate, bitrate, size, mono: ((h >>> 6) & 3) === 3 }
}

// Mirrors how Media3's Mp3Extractor seeks, as Voice configures it, and returns the largest gap
// between the time a seek reports and the time of the audio that then plays.
export async function checkSeeking(file: File, onProgress: (progress: number) => void): Promise<SeekCheck> {
  const chunkSize = 8 << 20
  let chunk = new DataView(new ArrayBuffer(0))
  let chunkStart = 0
  const load = async (pos: number) => {
    chunkStart = pos
    chunk = new DataView(await file.slice(pos, pos + chunkSize).arrayBuffer())
    onProgress(pos / file.size)
  }

  await load(0)
  let pos = 0
  // Like Media3, skip every ID3 tag in front of the audio, not only the first.
  while (true) {
    if (pos + 10 > chunkStart + chunk.byteLength) await load(pos)
    const at = pos - chunkStart
    if (chunk.byteLength - at < 10 || chunk.getUint32(at) >>> 8 !== 0x494433) break
    const size = [6, 7, 8, 9].reduce((sum, i) => (sum << 7) | (chunk.getUint8(at + i) & 0x7f), 0)
    pos += 10 + size + (chunk.getUint8(at + 5) & 0x10 ? 10 : 0)
  }

  const audioStart = pos
  let first: (Header & { pos: number; bytes: Uint8Array }) | undefined
  let offsets = new Float64Array(1 << 16)
  let count = 0
  let variableBitrate = false
  while (pos + 4 <= file.size) {
    if (pos + 4 > chunkStart + chunk.byteLength) await load(pos)
    const frame = header(chunk, pos - chunkStart)
    if (!frame || (first && (frame.sampleRate !== first.sampleRate || frame.mpeg1 !== first.mpeg1))) {
      // Media3 gives up looking for the first frame after the same number of bytes.
      if (!first && pos - audioStart > 128 * 1024) return { kind: 'not-mp3' }
      pos++
      continue
    }
    if (!first) {
      if (pos + frame.size + 4 > chunkStart + chunk.byteLength) await load(pos)
      const start = pos - chunkStart
      const next = start + frame.size + 4 <= chunk.byteLength ? header(chunk, start + frame.size) : null
      if (next?.sampleRate !== frame.sampleRate) {
        pos++
        continue
      }
      first = { ...frame, pos, bytes: new Uint8Array(chunk.buffer.slice(start, start + frame.size)) }
    }
    if (frame.bitrate !== first.bitrate) variableBitrate = true
    if (count === offsets.length) {
      const grown = new Float64Array(count * 2)
      grown.set(offsets)
      offsets = grown
    }
    offsets[count++] = pos
    pos += frame.size
  }
  if (!first || count < 2) return { kind: 'not-mp3' }

  const text = (at: number) => String.fromCharCode(...first.bytes.subarray(at, at + 4))
  const firstView = new DataView(first.bytes.buffer, first.bytes.byteOffset, first.bytes.byteLength)
  const xingAt = first.mpeg1 ? (first.mono ? 21 : 36) : first.mono ? 13 : 21
  const seekFrame = ['Xing', 'Info'].includes(text(xingAt)) ? text(xingAt) : text(36) === 'VBRI' ? 'VBRI' : null
  const audio = seekFrame ? offsets.subarray(1, count) : offsets.subarray(0, count)
  const frameSeconds = (first.mpeg1 ? 1152 : 576) / first.sampleRate
  const duration = audio.length * frameSeconds
  const dataStart = audio[0]

  let seek: (time: number) => { time: number; pos: number }
  const constantBitrate = (start: number, bitrate: number) => (time: number) => {
    const pos = start + Math.floor((time * bitrate) / 8)
    return { time: ((pos - start) * 8) / bitrate, pos }
  }
  if (seekFrame === 'Xing' || seekFrame === 'Info') {
    const flags = firstView.getUint32(xingAt + 4)
    let at = xingAt + 8
    const frames = flags & 1 ? firstView.getUint32(at) : undefined
    if (flags & 1) at += 4
    const bytes = flags & 2 ? firstView.getUint32(at) : undefined
    if (flags & 2) at += 4
    const dataSize = Math.min(bytes ?? Infinity, file.size - first.pos)
    const xingDuration = frames ? frames * frameSeconds : duration
    if (seekFrame === 'Xing' && flags & 4 && at + 100 <= first.bytes.length) {
      const toc = first.bytes.subarray(at, at + 100)
      seek = (target) => {
        const time = Math.min(target, xingDuration)
        const percent = (time * 100) / xingDuration
        const index = Math.min(Math.floor(percent), 99)
        const next = index === 99 ? 256 : toc[index + 1]
        const scaled = toc[index] + (percent - index) * (next - toc[index])
        return { time, pos: first.pos + Math.min(Math.max(Math.round((scaled / 256) * dataSize), first.size), dataSize - 1) }
      }
    } else {
      seek = constantBitrate(dataStart, ((dataSize - first.size) * 8) / xingDuration)
    }
  } else if (
    seekFrame === 'VBRI' &&
    first.bytes.length >= 62 + (firstView.getUint16(54) - 1) * firstView.getUint16(58)
  ) {
    const entries = firstView.getUint16(54)
    const scale = firstView.getUint16(56)
    const entrySize = firstView.getUint16(58)
    const positions = [dataStart]
    for (let i = 0; i < entries - 1; i++) {
      const at = 62 + i * entrySize
      let size = 0
      for (let b = 0; b < entrySize; b++) size = size * 256 + first.bytes[at + b]
      positions.push(positions[i] + size * scale)
    }
    seek = (time) => {
      const index = Math.min(Math.floor((time * entries) / duration), entries - 1)
      return { time: (index * duration) / entries, pos: positions[index] }
    }
  } else {
    seek = constantBitrate(dataStart, first.bitrate)
  }

  let errorSeconds = 0
  for (let i = 1; i < 1000; i++) {
    const target = seek((duration * i) / 1000)
    let low = 0
    let high = audio.length - 1
    while (low < high) {
      const mid = (low + high) >> 1
      if (audio[mid] < target.pos) low = mid + 1
      else high = mid
    }
    errorSeconds = Math.max(errorSeconds, Math.abs(low * frameSeconds - target.time))
  }
  onProgress(1)
  return { kind: 'mp3', errorSeconds, variableBitrate, duration }
}
