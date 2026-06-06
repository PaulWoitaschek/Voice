import { defineConfig, type Plugin } from 'vite'
import { readdirSync, readFileSync, existsSync } from 'node:fs'
import { resolve, join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import yaml from 'js-yaml'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = resolve(HERE, '..')
const FASTLANE_DIR = resolve(REPO_ROOT, 'fastlane/metadata/android')
const RAW_DIR = resolve(HERE, 'public/raw')

function listRawScreenshots(formFactor: string): string[] {
  const dir = join(RAW_DIR, formFactor)
  if (!existsSync(dir)) return []
  return readdirSync(dir)
    .filter((f) => f.toLowerCase().endsWith('.png'))
    .sort((a, b) => a.localeCompare(b, 'en', { numeric: true }))
}

function readMarketing(locale: string): unknown {
  const file = join(FASTLANE_DIR, locale, 'marketing.yml')
  return yaml.load(readFileSync(file, 'utf8'))
}

function fastlanePlugin(): Plugin {
  return {
    name: 'voice-artwork-fastlane',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (!req.url) return next()
        const m = req.url.match(/^\/api\/marketing\/([^/?]+)\.json/)
        if (m) {
          try {
            const data = readMarketing(decodeURIComponent(m[1]))
            res.setHeader('content-type', 'application/json')
            res.end(JSON.stringify(data))
          } catch (err) {
            res.statusCode = 404
            res.end(String(err))
          }
          return
        }
        const r = req.url.match(/^\/api\/raw\/([^/?]+)\.json/)
        if (r) {
          res.setHeader('content-type', 'application/json')
          res.end(JSON.stringify(listRawScreenshots(decodeURIComponent(r[1]))))
          return
        }
        next()
      })
    },
  }
}

export default defineConfig({
  root: resolve(HERE, 'src'),
  publicDir: resolve(HERE, 'public'),
  server: {
    fs: {
      allow: [REPO_ROOT],
    },
    port: 5173,
    strictPort: true,
  },
  plugins: [fastlanePlugin()],
})
