import { readdirSync, existsSync, statSync } from 'node:fs'
import { join, resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = resolve(HERE, '../../..')
export const FASTLANE_DIR = join(REPO_ROOT, 'fastlane/metadata/android')

export function listLocaleDirs(): string[] {
  return readdirSync(FASTLANE_DIR)
    .filter((entry) => {
      const dir = join(FASTLANE_DIR, entry)
      return statSync(dir).isDirectory()
    })
    .sort()
}

export function listLocales(): string[] {
  return listLocaleDirs()
    .filter((locale) => existsSync(join(FASTLANE_DIR, locale, 'marketing.yml')))
}

export function imagesDir(locale: string): string {
  return join(FASTLANE_DIR, locale, 'images')
}
