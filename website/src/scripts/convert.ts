import {
  ALL_FORMATS,
  BlobSource,
  BufferTarget,
  canEncodeAudio,
  Conversion,
  Input,
  Mp4OutputFormat,
  Output,
} from 'mediabunny'
import { type Chapter, chplBox, id3Chapters, withBox } from './chapters'

export async function startConversion(file: File, onProgress: (progress: number) => void) {
  if (!(await canEncodeAudio('aac'))) {
    const { registerAacEncoder } = await import('@mediabunny/aac-encoder')
    registerAacEncoder()
  }

  const header = new Uint8Array(await file.slice(0, 4).arrayBuffer())
  const id3Version = String.fromCharCode(...header.subarray(0, 3)) === 'ID3' ? header[3] : 0
  let chapters: Chapter[] = []
  let moov: { data: Uint8Array; position: number } | undefined
  const target = new BufferTarget()
  const conversion = await Conversion.init({
    input: new Input({ source: new BlobSource(file), formats: ALL_FORMATS }),
    output: new Output({
      target,
      format: new Mp4OutputFormat({
        fastStart: false,
        onMoov: (data, position) => (moov = { data: data.slice(), position }),
      }),
    }),
    video: { discard: true },
    // Browser AAC encoders often reject the low sample rates of speech MP3 files.
    audio: async (track) => ({
      codec: 'aac',
      bitrate: 64_000,
      numberOfChannels: Math.min(await track.getNumberOfChannels(), 2),
      sampleRate: 44_100,
    }),
    // Raw ID3 frames would end up as unknown MP4 tags, and MP4 only stores a front cover.
    tags: ({ raw, images, ...tags }) => {
      chapters = id3Chapters(raw?.CHAP, id3Version)
      const cover = images?.find((image) => image.kind === 'coverFront') ?? images?.[0]
      return { ...tags, images: cover && [{ ...cover, kind: 'coverFront' }] }
    },
  })
  if (!conversion.isValid) throw new Error('No audio track')
  conversion.onProgress = onProgress

  const result = conversion.execute().then(() => {
    const bytes = new Uint8Array(target.buffer!)
    const parts: BlobPart[] =
      chapters.length > 0 && moov
        ? [bytes.subarray(0, moov.position), withBox(moov.data, chplBox(chapters))]
        : [bytes]
    return { blob: new Blob(parts, { type: 'audio/mp4' }), chapters: chapters.length }
  })
  return { conversion, result }
}
