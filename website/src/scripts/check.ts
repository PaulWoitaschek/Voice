import { ALL_FORMATS, type AudioCodec, BlobSource, Input, Mp3InputFormat, PCM_AUDIO_CODECS } from 'mediabunny'
import { id3Chapters, id3Version } from './chapters'
import { checkSeeking } from './seek-check'

export type Verdict = 'unreadable' | 'format' | 'seek' | 'fine'
export type FileCheck = { verdict: Verdict; format?: string; amount?: string; canConvert: boolean; details: string }

const DRM = ['aavd', 'enca', 'drms']
const PLAYABLE: AudioCodec[] = ['aac', 'mp3', 'opus', 'vorbis', 'flac', ...PCM_AUDIO_CODECS]
const NAMES: Partial<Record<AudioCodec, string>> = {
  aac: 'AAC',
  mp3: 'MP3',
  opus: 'Opus',
  vorbis: 'Vorbis',
  flac: 'FLAC',
  ac3: 'AC-3',
  eac3: 'E-AC-3',
  dts: 'DTS',
  truehd: 'TrueHD',
}

export async function checkFile(file: File, onProgress: (progress: number) => void): Promise<FileCheck> {
  const input = new Input({ source: new BlobSource(file), formats: ALL_FORMATS })
  try {
    const track = (await input.canRead()) ? await input.getPrimaryAudioTrack() : null
    const codec = await track?.getCodec()
    if (!track || !codec || DRM.includes(String(await track.getInternalCodecId()))) return unreadable(file)

    const format = await input.getFormat()
    const sampleRate = await track.getSampleRate()
    const channels = await track.getNumberOfChannels()
    let length: number | undefined
    const name = NAMES[codec] ?? 'PCM'
    let audio = `${name}, ${sampleRate / 1000} kHz, ${plural(channels, 'channel')}`
    let chapters: string[] = []
    let verdict: Verdict = PLAYABLE.includes(codec) ? 'fine' : 'format'
    let result = verdict === 'fine' ? 'no known problem' : `Voice can't play ${name}`
    let amount: string | undefined
    let tagsReadable = true

    if (format instanceof Mp3InputFormat) {
      const seek = await checkSeeking(file, onProgress)
      if (seek.kind === 'mp3') {
        audio += seek.variableBitrate ? ', variable bitrate' : ', constant bitrate'
        length = seek.duration
        if (seek.errorSeconds >= 5) {
          verdict = 'seek'
          amount = duration(seek.errorSeconds)
          result = `a jump can land up to ${amount} off`
        }
      }
      try {
        const tags = await input.getMetadataTags()
        chapters = [`Chapters: ${id3Chapters(tags.raw?.CHAP, await id3Version(file)).length}`]
      } catch (e) {
        console.error(e)
        tagsReadable = false
      }
    }
    length ??= await track.computeDuration()

    // The converter reads the same tags and fails on them.
    const canConvert = verdict !== 'fine' && tagsReadable && (await track.canDecode())
    const details = [
      `Type: ${format.name}`,
      `Audio: ${audio}`,
      `Length: ${clock(length)}`,
      size(file),
      ...chapters,
      `Result: ${result}`,
    ]
    return { verdict, format: name, amount, canConvert, details: details.join('\n') }
  } catch (e) {
    console.error(e)
    return unreadable(file)
  } finally {
    input.dispose()
  }
}

// Only known extensions, so that no part of a file name ends up in a public bug report.
const EXTENSIONS = ['aac', 'aax', 'aaxc', 'ac3', 'aiff', 'alac', 'amr', 'ape', 'flac', 'm4a', 'm4b', 'm4p', 'mka', 'mkv', 'mp3', 'mp4', 'ogg', 'opus', 'wav', 'webm', 'wma']

function unreadable(file: File): FileCheck {
  const extension = file.name.match(/\.([^.]+)$/)?.[1].toLowerCase()
  const type = extension && EXTENSIONS.includes(extension) ? extension.toUpperCase() : 'unknown'
  return { verdict: 'unreadable', canConvert: false, details: [`Type: ${type}`, size(file), "Result: can't read"].join('\n') }
}

const size = (file: File) => `Size: ${(file.size / 1e6).toFixed(file.size < 1e7 ? 1 : 0)} MB`

const clock = (seconds: number) => {
  const s = Math.round(seconds)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${Math.floor(s / 3600)}:${pad(Math.floor(s / 60) % 60)}:${pad(s % 60)}`
}

export const plural = (value: number, word: string) => `${value} ${value === 1 ? word : `${word}s`}`

const duration = (seconds: number) =>
  seconds < 90 ? plural(Math.max(1, Math.round(seconds)), 'second') : plural(Math.round(seconds / 60), 'minute')
