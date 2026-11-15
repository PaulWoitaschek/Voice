import { checkFile } from './check'

self.onmessage = async ({ data }: MessageEvent<File>) => {
  const result = await checkFile(data, (progress) => postMessage({ progress }))
  postMessage({ result })
}
