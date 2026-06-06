import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import yaml from 'js-yaml'
import { FASTLANE_DIR } from './locales.ts'

export interface MarketingStrings {
  featureGraphic: { headline: string; tagline: string }
  screenshots: { caption: string }[]
}

export function readMarketing(locale: string): MarketingStrings {
  const file = join(FASTLANE_DIR, locale, 'marketing.yml')
  return yaml.load(readFileSync(file, 'utf8')) as MarketingStrings
}
