import { defineConfig } from 'astro/config'

const github = 'https://github.com/PaulWoitaschek/Voice/blob/main'

export default defineConfig({
  site: 'https://voice.woitaschek.de',
  trailingSlash: 'always',
  redirects: {
    '/privacy_policy/': '/privacy-policy/',
    '/privacy/': '/privacy-policy/',
    '/download-sources/': '/#download',
    '/organizing/': '/faq/#folders',
    '/about/': '/',
    '/license/': `${github}/LICENSE.md`,
    '/contributing/': `${github}/CONTRIBUTING.md`,
    '/development/': `${github}/docs/development.md`,
    '/architecture/': `${github}/docs/architecture.md`,
    '/product/': `${github}/docs/product.md`,
    '/library/': `${github}/docs/library.md`,
  },
  markdown: { syntaxHighlight: false },
  vite: {
    server: {
      // Only what the site shows from outside website/, as the repository root holds signing and local files.
      fs: { allow: ['.', '../artwork/public', '../fastlane/metadata/android/en-US/images'] },
    },
  },
})
