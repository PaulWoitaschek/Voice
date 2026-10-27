'use strict';

const root = document.getElementById('app');
const notesEl = document.getElementById('notes');
const SPEC = 'https://github.com/PaulWoitaschek/Voice/blob/main/docs/library.md';
const DAY = 864e5;
const collator = new Intl.Collator('en', { numeric: true, sensitivity: 'base' });
const byName = (a, b) => collator.compare(a, b);
const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)');
const darkQuery = matchMedia('(prefers-color-scheme: dark)');

const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const hsl = (h, s, l) => `hsl(${((h % 360) + 360) % 360} ${s}% ${l}%)`;
const plural = (n, one, other = `${one}s`) => `${n.toLocaleString('en')} ${n === 1 ? one : other}`;
const percent = fraction => `${Math.round(fraction * 100)}%`;
const fold = s => s.normalize('NFKD').replace(/[̀-ͯ]/g, '').toLowerCase();
const norm = s => fold(s).replace(/[^\p{L}\p{N}]/gu, '');
const words = s => (s ? fold(s).split(/[^\p{L}\p{N}]+/u).filter(Boolean) : []);
const parseTime = t => t.split(':').map(Number).reduce((sum, part) => sum * 60 + part, 0);
const isNumber = s => /^\d+$/.test(s);

function fmtTime(seconds) {
  const s = Math.max(0, Math.round(seconds));
  return `${Math.floor(s / 3600)}:${pad(Math.floor((s % 3600) / 60), 2)}:${pad(s % 60, 2)}`;
}

const ICONS = {
  search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  sort: 'M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z',
  settings: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  close: 'M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  more: 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
  chevron: 'M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z',
  down: 'M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z',
  play: 'M8 6.82v10.36c0 .79.87 1.27 1.54.84l8.14-5.18a1 1 0 0 0 0-1.69L9.54 5.98A.998.998 0 0 0 8 6.82z',
  pause: 'M8 19c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2s-2 .9-2 2v10c0 1.1.9 2 2 2zm6-12v10c0 1.1.9 2 2 2s2-.9 2-2V7c0-1.1-.9-2-2-2s-2 .9-2 2z',
  check: 'M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z',
  hidden: 'M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.8 11.8 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z',
  visible: 'M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z',
  edit: 'M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
  public: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z',
  image: 'M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z',
  folder: 'M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z',
  delete: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
  select: 'M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm-9 14-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
  person: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z',
  stack: 'M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z',
  replay: 'M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z',
  dns: 'M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z',
  phone: 'M17 1.01 7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z',
  split: 'M14 4l2.29 2.29-2.88 2.88 1.42 1.42 2.88-2.88L20 10V4zm-4 0H4v6l2.29-2.29 4.71 4.7V20h2v-8.41l-5.29-5.3z',
  merge: 'M17 20.41 18.41 19 15 15.59 13.59 17 17 20.41zM7.5 8H11v5.59L5.59 19 7 20.41l6-6V8h3.5L12 3.5 7.5 8z',
  add: 'M20 6h-8l-2-2H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-1 8h-3v3h-2v-3h-3v-2h3V9h2v3h3v2z',
  grid: 'M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z',
  list: 'M3 14h4v-4H3v4zm0 5h4v-4H3v4zM3 9h4V5H3v4zm5 5h13v-4H8v4zm0 5h13v-4H8v4zM8 5v4h13V5H8z',
  hourglass: 'M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2H6zm10 14.5V20H8v-3.5l4-4 4 4zm-4-5-4-4V4h8v3.5l-4 4z',
  headphones: 'M12 3a9 9 0 0 0-9 9v7c0 1.1.9 2 2 2h4v-8H5v-1c0-3.87 3.13-7 7-7s7 3.13 7 7v1h-4v8h4c1.1 0 2-.9 2-2v-7a9 9 0 0 0-9-9z',
  restart: 'M12 5V2L8 6l4 4V7c3.31 0 6 2.69 6 6 0 2.97-2.17 5.43-5 5.91v2.02c3.95-.49 7-3.85 7-7.93 0-4.42-3.58-8-8-8zm-6 8c0-1.65.67-3.15 1.76-4.24L6.34 7.34A7.956 7.956 0 0 0 4 13c0 4.08 3.05 7.44 7 7.93v-2.02c-2.83-.48-5-2.94-5-5.91z',
};
const icon = (name, cls = 'ic') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICONS[name]}"/></svg>`;

function polar(points, depth, sharpness = 1) {
  let d = '';
  for (let i = 0; i <= 160; i++) {
    const t = (i / 160) * Math.PI * 2;
    let c = Math.cos(points * t);
    if (sharpness !== 1) c = Math.sign(c) * Math.abs(c) ** sharpness;
    const r = 50 * (1 - (depth * (1 - c)) / 2);
    d += `${i ? 'L' : 'M'}${(50 + r * Math.cos(t - Math.PI / 2)).toFixed(2)} ${(50 + r * Math.sin(t - Math.PI / 2)).toFixed(2)}`;
  }
  return `${d}Z`;
}

function squircle() {
  let d = '';
  for (let i = 0; i <= 160; i++) {
    const t = (i / 160) * Math.PI * 2;
    const r = 48 / (Math.abs(Math.cos(t)) ** 5 + Math.abs(Math.sin(t)) ** 5) ** (1 / 5);
    d += `${i ? 'L' : 'M'}${(50 + r * Math.cos(t)).toFixed(2)} ${(50 + r * Math.sin(t)).toFixed(2)}`;
  }
  return `${d}Z`;
}

const SHAPES = {
  circle: polar(1, 0),
  cookie4: polar(4, 0.16),
  cookie6: polar(6, 0.13),
  cookie9: polar(9, 0.1),
  clover: polar(4, 0.34, 0.45),
  sunny: polar(8, 0.16),
  burst: polar(10, 0.14),
  pentagon: polar(5, 0.16, 1.6),
  flower: polar(8, 0.24, 0.6),
  gear: polar(12, 0.24, 0.3),
  squircle: squircle(),
};
const shape = (name, cls = 'shape') => `<svg class="${cls}" viewBox="0 0 100 100" aria-hidden="true"><path d="${SHAPES[name]}"/></svg>`;
const shaped = (name, bg, fg, content) => `<span class="shaped" style="--bg:var(--${bg});--fg:var(--${fg})">${shape(name)}${ICONS[content] ? icon(content) : `<span class="initials">${esc(content)}</span>`}</span>`;

function playButton(playing, cls, action, attrs = '') {
  return `<button class="play-btn ${cls} ${playing ? 'playing' : ''} ${anim.morph ? 'morph' : ''}" data-action="${action}" ${attrs} aria-label="${playing ? 'Pause' : 'Play'}">${shape(playing ? 'squircle' : 'cookie9')}${icon(playing ? 'pause' : 'play')}</button>`;
}

function coverPalette(b) {
  const h = b.hue ?? 210;
  return b.light
    ? { c1: hsl(h, 62, 87), c2: hsl(h + 24, 52, 72), ink: hsl(h, 50, 16), accent: hsl(h + 190, 55, 40), soft: hsl(h + 24, 40, 62) }
    : { c1: hsl(h, 52, 34), c2: hsl(h + 24, 62, 15), ink: hsl(h, 40, 96), accent: hsl(h + 35, 85, 72), soft: hsl(h, 40, 46) };
}

function seeded(text) {
  let x = 7;
  for (const c of text) x = (Math.imul(x, 31) + c.charCodeAt(0)) >>> 0;
  return () => (x = (Math.imul(x, 1103515245) + 12345) >>> 0) / 2 ** 32;
}

function motif(b, p) {
  const a = p.accent;
  switch (b.motif) {
    case 'signal':
      return `<g fill="none" stroke="${a}" stroke-width="2.6" stroke-linecap="round">${[10, 19, 28].map(r => `<path d="M${50 - r} 84A${r} ${r} 0 0 1 ${50 + r} 84"/>`).join('')}</g><circle cx="50" cy="84" r="3.2" fill="${a}"/>`;
    case 'planet':
      return `<circle cx="62" cy="64" r="17" fill="${a}"/><ellipse cx="62" cy="64" rx="31" ry="7" fill="none" stroke="${p.ink}" stroke-width="1.6" opacity=".75" transform="rotate(-18 62 64)"/>`;
    case 'orbit':
      return `<g fill="none" stroke="${a}" stroke-width="1.6"><ellipse cx="50" cy="66" rx="32" ry="11" transform="rotate(20 50 66)"/><ellipse cx="50" cy="66" rx="32" ry="11" transform="rotate(-20 50 66)"/></g><circle cx="50" cy="66" r="7" fill="${a}"/>`;
    case 'stars': {
      const r = seeded(b.id);
      return Array.from({ length: 16 }, () => `<circle cx="${(r() * 92 + 4).toFixed(1)}" cy="${(r() * 46 + 46).toFixed(1)}" r="${(r() * 1.3 + 0.5).toFixed(2)}" fill="${p.ink}" opacity="${(r() * 0.5 + 0.4).toFixed(2)}"/>`).join('')
        + `<path d="M70 60l2.2 6.3 6.3 2.2-6.3 2.2L70 77l-2.2-6.3-6.3-2.2 6.3-2.2z" fill="${a}"/>`;
    }
    case 'moon':
      return `<path d="M60 46A16 16 0 1 0 76 62A16 16 0 0 1 60 46Z" fill="${a}"/>`;
    case 'house':
      return `<path d="M24 62L50 40 76 62Z" fill="${p.ink}" opacity=".9"/><rect x="30" y="60" width="40" height="28" fill="${a}"/><rect x="36" y="66" width="9" height="9" fill="${p.c2}"/><rect x="55" y="66" width="9" height="9" fill="${p.c2}"/><rect x="45" y="76" width="10" height="12" fill="${p.c2}"/>`;
    case 'lighthouse':
      return `<path d="M50 50L94 32V68Z" fill="${a}" opacity=".35"/><path d="M45 92L47.5 54h5L55 92Z" fill="${p.ink}"/><rect x="45.5" y="47" width="9" height="7" rx="1.5" fill="${a}"/><path d="M44 47l6-5 6 5Z" fill="${p.ink}"/>`;
    case 'lantern':
      return `<circle cx="50" cy="66" r="24" fill="${a}" opacity=".18"/><circle cx="50" cy="66" r="15" fill="${a}" opacity=".3"/><rect x="42" y="56" width="16" height="22" rx="4" fill="${a}"/><path d="M45 56v-4h10v4M50 52v-4" stroke="${p.ink}" stroke-width="1.6" fill="none"/>`;
    case 'mountains':
      return `<path d="M0 90L26 58 40 72 64 44 100 88V100H0Z" fill="${a}" opacity=".9"/><path d="M0 100L30 78 52 92 76 74 100 92V100Z" fill="${p.soft}"/>`;
    case 'gear':
      return `<g transform="translate(30 46) scale(.4)"><path d="${SHAPES.gear}" fill="${a}"/></g><circle cx="50" cy="66" r="7" fill="${p.c2}"/>`;
    case 'waves':
      return `<g fill="none" stroke="${a}" stroke-width="2.4" stroke-linecap="round">${[64, 74, 84].map(y => `<path d="M-2 ${y}q8-6 16 0t16 0 16 0 16 0 16 0 16 0 16 0"/>`).join('')}</g>`;
    case 'sun':
      return `<circle cx="50" cy="70" r="18" fill="${a}"/><rect y="74" width="100" height="26" fill="${p.c2}" opacity=".85"/><path d="M14 80h72M24 87h52M34 94h32" stroke="${a}" stroke-width="2" stroke-linecap="round" opacity=".7"/>`;
    case 'rain':
      return `<g stroke="${a}" stroke-width="1.6" stroke-linecap="round" opacity=".8">${Array.from({ length: 14 }, (_, i) => `<path d="M${((i * 7.3) % 96) + 4} ${50 + ((i * 13) % 38)}l-3 8"/>`).join('')}</g>`;
    case 'compass':
      return `<circle cx="50" cy="66" r="20" fill="none" stroke="${a}" stroke-width="2"/><path d="M50 48l5 18-5 18-5-18Z" fill="${a}"/><circle cx="50" cy="66" r="2" fill="${p.c2}"/>`;
    case 'crown':
      return `<path d="M28 82V58l11 10 11-16 11 16 11-10v24Z" fill="${a}"/>`;
    case 'leaf':
      return `<path d="M50 90C30 76 30 52 50 40c20 12 20 36 0 50Z" fill="${a}"/><path d="M50 44v46" stroke="${p.c2}" stroke-width="1.6"/>`;
    case 'lines':
      return `<g stroke="${a}" stroke-width="3" opacity=".7">${[0, 1, 2, 3, 4, 5, 6].map(i => `<path d="M${-24 + i * 18} 100L${26 + i * 18} 50"/>`).join('')}</g>`;
    case 'episode':
      return `<path d="M85 78A7 7 0 1 0 92 85A7 7 0 0 1 85 78Z" fill="${a}" opacity=".8"/>`;
    default:
      return '';
  }
}

function wrapTitle(text, max) {
  const lines = [];
  let line = '';
  for (const w of text.split(/\s+/)) {
    if (line && `${line} ${w}`.length > max) {
      lines.push(line);
      line = w;
    } else {
      line = line ? `${line} ${w}` : w;
    }
  }
  if (line) lines.push(line);
  if (lines.length > 3) {
    lines.length = 3;
    lines[2] += '…';
  }
  return lines;
}

const coverCache = new Map();

function coverSvg(b) {
  const title = titleOf(b);
  const author = authorOf(b);
  const cacheKey = `${b.id}|${title}|${author}|${seriesOf(b)}`;
  if (coverCache.has(cacheKey)) return coverCache.get(cacheKey);
  const p = coverPalette(b);
  let text;
  if (b.motif === 'episode') {
    const number = title.match(/\d+/)?.[0] ?? '';
    const name = title.includes(':') ? title.split(':').slice(1).join(':').trim() : '';
    text = `<text x="50" y="18" text-anchor="middle" font-size="6.2" letter-spacing=".9" font-weight="600" fill="${p.ink}">${esc((seriesOf(b) ?? '').toUpperCase())}</text>`
      + `<text x="50" y="62" text-anchor="middle" font-size="30" font-weight="700" fill="${p.accent}">${esc(number)}</text>`
      + (name ? `<text x="50" y="76" text-anchor="middle" font-size="5.6" fill="${p.ink}">${esc(name.length > 26 ? `${name.slice(0, 25)}…` : name)}</text>` : '');
  } else {
    const lines = wrapTitle(title, 13);
    const longest = Math.max(...lines.map(l => l.length));
    const size = longest <= 8 ? 12 : longest <= 11 ? 10.5 : 9;
    text = `<text x="50" y="${14 + size}" text-anchor="middle" font-size="${size}" font-weight="600" fill="${p.ink}">${lines.map((l, i) => `<tspan x="50" dy="${i ? size * 1.12 : 0}">${esc(l)}</tspan>`).join('')}</text>`;
  }
  const byline = author ? `<text x="50" y="95" text-anchor="middle" font-size="4.4" letter-spacing=".9" fill="${p.ink}" opacity=".8">${esc(author.toUpperCase())}</text>` : '';
  const svg = `<svg viewBox="0 0 100 100" aria-hidden="true" font-family="Literata, Georgia, serif">${motif(b, p)}${text}${byline}</svg>`;
  coverCache.set(cacheKey, svg);
  return svg;
}

function cover(b) {
  const p = coverPalette(b);
  return `<div class="cover" style="background:linear-gradient(150deg,${p.c1},${p.c2})">${coverSvg(b)}</div>`;
}

// Like CoverTheme in the app: the colors of the book-centric surfaces come from the cover.
function coverVars(b) {
  if (!b) return '';
  const h = b.hue ?? 210;
  const v = isDark()
    ? { primary: hsl(h, 75, 80), onPrimary: hsl(h, 60, 16), pc: hsl(h, 38, 30), onPc: hsl(h, 60, 90), tc: hsl(h + 50, 30, 27), sc: hsl(h, 25, 26), onSc: hsl(h, 40, 88), low: hsl(h, 14, 12), c: hsl(h, 14, 15), high: hsl(h, 14, 19), highest: hsl(h, 14, 24) }
    : { primary: hsl(h, 58, 34), onPrimary: '#fff', pc: hsl(h, 80, 88), onPc: hsl(h, 60, 18), tc: hsl(h + 50, 70, 89), sc: hsl(h, 50, 90), onSc: hsl(h, 40, 22), low: hsl(h, 40, 96), c: hsl(h, 34, 94), high: hsl(h, 30, 92), highest: hsl(h, 26, 88) };
  return `--primary:${v.primary};--on-primary:${v.onPrimary};--primary-container:${v.pc};--on-primary-container:${v.onPc};--tertiary-container:${v.tc};--secondary-container:${v.sc};--on-secondary-container:${v.onSc};--surface-low:${v.low};--surface-c:${v.c};--surface-high:${v.high};--surface-highest:${v.highest}`;
}

function makeBook(fields) {
  return {
    title: '', author: null, series: null, part: null, file: fields.title, narrator: null, genre: null,
    duration: 3600, position: 0, finished: false, lastListenedAt: null, addedAt: null, source: 'audiobooks',
    hidden: false, hue: 210, light: false, motif: 'lines', number: null, folderKey: null, fromFolder: null, edit: {},
    ...fields,
  };
}

function hydrate(raw, now) {
  const duration = parseTime(raw.duration);
  let position = 0;
  if (raw.finished) position = duration;
  else if (raw.left) position = duration - parseTime(raw.left);
  else if (raw.position) position = parseTime(raw.position);
  else if (raw.progress) position = Math.round(duration * raw.progress);
  return makeBook({
    ...raw,
    duration,
    position,
    finished: !!raw.finished,
    lastListenedAt: raw.last != null ? now - raw.last * DAY : null,
    addedAt: raw.added != null ? now - raw.added * DAY : null,
    file: raw.file ?? raw.title,
    author: raw.author ?? null,
  });
}

let state;
let index = null;

function freshState(prefs) {
  const now = Date.now();
  return {
    version: 0,
    books: seedBooks().map(raw => hydrate(raw, now)),
    folders: structuredClone(FOLDERS),
    sources: SOURCES.map(s => ({ ...s })),
    prefs: { layout: 'list', theme: 'system', ...prefs, sort: 'progress', showFinished: true, showHidden: false, off: new Set() },
    currentBookId: 'signal-2',
    playing: false,
    nav: [{ screen: 'library' }],
    sheet: null,
    menu: null,
    selection: null,
    search: { query: '', filter: 'all', browse: 'authors' },
    snackbar: null,
    tasks: new Set(),
    preview: null,
  };
}

const changed = () => state.version++;
const value = (b, key) => (key in b.edit ? b.edit[key] : b[key]);
const titleOf = b => value(b, 'title');
const seriesOf = b => value(b, 'series') || null;
const partOf = b => value(b, 'part') ?? null;
const statusOf = b => (b.finished ? 'finished' : b.position > 0 ? 'current' : 'notStarted');
const bookById = id => state.books.find(b => b.id === id);
const sourceOf = b => state.sources.find(s => s.id === b.source);
const isServer = b => !!sourceOf(b)?.server;

function authorOf(b) {
  const a = value(b, 'author')?.trim();
  return a && !/^<?unknown>?$/i.test(a) ? a : null;
}
const authorKey = b => (authorOf(b) ? norm(authorOf(b)) || null : null);
const seriesKey = b => (seriesOf(b) ? norm(seriesOf(b)) || null : null);

function seriesOrder(a, b) {
  const pa = partOf(a);
  const pb = partOf(b);
  if (pa != null && pb != null) return pa - pb || byName(a.file, b.file);
  if (pa != null) return -1;
  if (pb != null) return 1;
  return byName(a.file, b.file);
}

function getIndex() {
  if (index?.version === state.version) return index;
  const authors = new Map();
  const series = new Map();
  const add = (map, name, b) => {
    const key = norm(name);
    if (!key) return;
    if (!map.has(key)) map.set(key, { key, spellings: new Map(), books: [] });
    const entry = map.get(key);
    entry.books.push(b);
    entry.spellings.set(name, (entry.spellings.get(name) ?? 0) + 1);
  };
  for (const b of state.books) {
    if (authorOf(b)) add(authors, authorOf(b), b);
    if (seriesOf(b)) add(series, seriesOf(b), b);
  }
  const spelling = entry => [...entry.spellings].sort((x, y) => y[1] - x[1])[0][0];
  for (const a of authors.values()) {
    a.name = spelling(a);
    a.books.sort((x, y) => byName(titleOf(x), titleOf(y)));
  }
  for (const s of series.values()) {
    s.name = spelling(s);
    s.books.sort(seriesOrder);
    const counts = new Map();
    for (const b of s.books) if (authorKey(b)) counts.set(authorKey(b), (counts.get(authorKey(b)) ?? 0) + 1);
    s.authorKey = [...counts].sort((x, y) => y[1] - x[1])[0]?.[0] ?? null;
  }
  index = { version: state.version, authors, series };
  return index;
}

function seriesLine(b) {
  const key = seriesKey(b);
  if (!key) return null;
  const part = partOf(b);
  return `${getIndex().series.get(key)?.name ?? seriesOf(b)}${part != null ? ` ${part}` : ''}`;
}

function nextInSeries(b) {
  const key = seriesKey(b);
  if (!key) return null;
  const books = getIndex().series.get(key).books.filter(x => !x.hidden);
  return books.slice(books.indexOf(b) + 1).find(x => !x.finished) ?? null;
}

const inLibrary = b => !state.prefs.off.has(b.source) && (state.prefs.showHidden || !b.hidden);
const bookUnit = b => ({ kind: 'book', book: b, books: [b] });
const seriesUnit = (key, books) => ({ kind: 'series', key, books });
const authorUnit = (key, units) => ({ kind: 'author', key, units, books: units.flatMap(u => u.books) });
const unitLast = u => Math.max(0, ...u.books.map(b => b.lastListenedAt ?? 0));
const allFinished = u => u.books.every(b => b.finished);
const nextOf = books => books.find(b => !b.finished) ?? books[0];
const seriesProgress = books => {
  const total = books.reduce((sum, b) => sum + b.duration, 0);
  return total ? books.reduce((sum, b) => sum + (b.finished ? b.duration : b.position), 0) / total : 0;
};

function unitName(u) {
  if (u.kind === 'book') return titleOf(u.book);
  return (u.kind === 'series' ? getIndex().series : getIndex().authors).get(u.key).name;
}
const byUnitName = (a, b) => byName(unitName(a), unitName(b));
const byLastListened = (a, b) => unitLast(b) - unitLast(a);

function unitAuthorKey(u) {
  return u.kind === 'book' ? authorKey(u.book) : getIndex().series.get(u.key).authorKey;
}

function libraryUnits() {
  const groups = new Map();
  const units = [];
  for (const b of state.books) {
    if (!inLibrary(b)) continue;
    const key = seriesKey(b);
    if (!key) {
      units.push(bookUnit(b));
      continue;
    }
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(b);
  }
  for (const [key, books] of groups) units.push(books.length >= 2 ? seriesUnit(key, books.sort(seriesOrder)) : bookUnit(books[0]));
  return units;
}

function heroModel() {
  const current = bookById(state.currentBookId);
  if (!current || !inLibrary(current)) return null;
  if (current.finished) {
    const next = nextInSeries(current);
    if (next) return { book: next, upNext: true };
  }
  return { book: current, upNext: false };
}

function addedLabel(timestamp) {
  if (timestamp == null) return 'Earlier';
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const date = new Date(timestamp);
  const day = new Date(timestamp);
  day.setHours(0, 0, 0, 0);
  const days = Math.round((today - day) / DAY);
  if (days <= 0) return 'Today';
  if (days === 1) return 'Yesterday';
  if (days < 30) return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'long' });
  const sameYear = date.getFullYear() === today.getFullYear();
  return date.toLocaleDateString('en-GB', sameYear ? { month: 'long' } : { month: 'long', year: 'numeric' });
}

function libraryModel() {
  const { sort, showFinished } = state.prefs;
  const hero = heroModel();
  const units = libraryUnits();
  const notHero = u => !(hero && u.kind === 'book' && u.book.id === hero.book.id);

  if (sort === 'progress') {
    const current = [];
    const notStarted = [];
    const completed = [];
    for (const u of units) {
      if (u.kind === 'book') {
        const status = statusOf(u.book);
        (status === 'current' ? current : status === 'finished' ? completed : notStarted).push(u);
        continue;
      }
      const inProgress = u.books.filter(b => statusOf(b) === 'current');
      if (inProgress.length) {
        current.push(...inProgress.map(bookUnit));
        continue;
      }
      const finished = u.books.filter(b => b.finished).length;
      (finished === u.books.length ? completed : finished === 0 ? notStarted : current).push(u);
    }
    return {
      hero,
      sections: [
        { title: 'Current', items: current.sort(byLastListened).filter(notHero) },
        { title: 'Not started yet', items: notStarted.sort(byUnitName) },
        ...(showFinished ? [{ title: 'Completed', items: completed.sort(byLastListened) }] : []),
      ],
    };
  }

  const current = units
    .flatMap(u => u.books.filter(b => statusOf(b) === 'current'))
    .map(bookUnit)
    .sort(byLastListened)
    .filter(notHero);
  const keep = u => showFinished || !allFinished(u);
  let groups;
  if (sort === 'title') {
    groups = [{ label: null, items: units.filter(keep).sort(byUnitName) }];
  } else if (sort === 'author') {
    const byAuthor = new Map();
    const noAuthor = [];
    for (const u of units) {
      const key = unitAuthorKey(u);
      if (!key) {
        noAuthor.push(u);
        continue;
      }
      if (!byAuthor.has(key)) byAuthor.set(key, []);
      byAuthor.get(key).push(u);
    }
    const authors = getIndex().authors;
    const items = [...byAuthor]
      .map(([key, us]) => ({ key, item: us.length >= 2 ? authorUnit(key, authorOrder(us)) : us[0] }))
      .filter(({ item }) => keep(item))
      .sort((a, b) => byName(authors.get(a.key).name, authors.get(b.key).name) || byUnitName(a.item, b.item))
      .map(({ item }) => item);
    groups = [{ label: null, items }, { label: 'No author', items: noAuthor.filter(keep).sort(byUnitName) }];
  } else {
    const byLabel = new Map();
    for (const u of units.filter(keep)) {
      const added = u.books.map(b => b.addedAt).filter(t => t != null);
      const newest = added.length ? Math.max(...added) : null;
      const label = addedLabel(newest);
      if (!byLabel.has(label)) byLabel.set(label, { label, newest: -Infinity, items: [] });
      const group = byLabel.get(label);
      group.newest = Math.max(group.newest, newest ?? -Infinity);
      group.items.push(u);
    }
    groups = [...byLabel.values()].sort((a, b) => b.newest - a.newest);
    groups.forEach(g => g.items.sort(byUnitName));
  }
  groups = groups.filter(g => g.items.length);
  const count = groups.reduce((sum, g) => sum + g.items.reduce((n, u) => n + u.books.length, 0), 0);
  return { hero, sections: [{ title: 'Current', items: current }, { title: 'All books', count, groups }] };
}

function authorOrder(units) {
  const series = units.filter(u => u.kind === 'series').sort(byUnitName);
  const books = units.filter(u => u.kind === 'book').sort(byUnitName);
  return [...series, ...books];
}

function authorModel(key) {
  const ix = getIndex();
  const author = ix.authors.get(key);
  if (!author) return null;
  const seriesKeys = new Set();
  const others = [];
  for (const b of author.books) {
    const s = ix.series.get(seriesKey(b) ?? '');
    if (s && s.authorKey === key && s.books.length >= 2) seriesKeys.add(s.key);
    else others.push(b);
  }
  const stacks = [...seriesKeys].map(k => seriesUnit(k, ix.series.get(k).books)).sort(byUnitName);
  const books = [...stacks.flatMap(u => u.books), ...others];
  return { key, name: author.name, stacks, others, books };
}

function seriesCta(s) {
  const visible = s.books.filter(b => !b.hidden);
  const number = b => partOf(b) ?? s.books.indexOf(b) + 1;
  const inProgress = visible.filter(b => statusOf(b) === 'current').sort((a, b) => (b.lastListenedAt ?? 0) - (a.lastListenedAt ?? 0))[0];
  if (inProgress) return { book: inProgress, label: `Continue book ${number(inProgress)}` };
  const next = visible.find(b => !b.finished);
  return next ? { book: next, label: `Start book ${number(next)}` } : null;
}

const matchWord = (word, token) => (isNumber(token) ? isNumber(word) && Number(word) === Number(token) : word.startsWith(token));
// narrators and genres match whole names, so "Mark Ellis" doesn't find "Mark Ellison"
const matchWhole = (word, token) => (isNumber(token) ? isNumber(word) && Number(word) === Number(token) : word === token);

function matchBook(b, tokens) {
  const title = words(titleOf(b));
  const author = words(authorOf(b));
  const series = words(seriesOf(b));
  const narrator = words(b.narrator);
  const genres = (b.genre ?? '').split(',').map(words);
  const part = partOf(b);
  const fields = new Set();
  let score = 0;
  for (const token of tokens) {
    let found = false;
    const hit = (field, points, ok) => {
      if (!ok) return;
      fields.add(field);
      score += points;
      found = true;
    };
    hit('title', 3, title.some(w => matchWord(w, token)));
    hit('series', 2, series.some(w => matchWord(w, token)));
    hit('series', 3, series.length > 0 && isNumber(token) && part === Number(token));
    hit('author', 1.5, author.some(w => matchWord(w, token)));
    hit('narrator', 1, narrator.some(w => matchWhole(w, token)));
    hit('genre', 1, genres.some(g => g.some(w => matchWhole(w, token))));
    if (!found) return null;
  }
  return { book: b, score, fields };
}

function searchModel(query) {
  const tokens = words(query);
  if (!tokens.length) return null;
  const inProgress = b => (statusOf(b) === 'current' ? 1 : 0);
  const books = state.books
    .map(b => matchBook(b, tokens))
    .filter(Boolean)
    .sort((x, y) => y.score - x.score || inProgress(y.book) - inProgress(x.book) || (y.book.lastListenedAt ?? 0) - (x.book.lastListenedAt ?? 0) || byName(titleOf(x.book), titleOf(y.book)));
  const named = name => tokens.every(t => words(name).some(w => matchWord(w, t)));
  const ix = getIndex();
  return {
    tokens,
    books,
    authors: [...ix.authors.values()].filter(a => named(a.name)).sort((a, b) => byName(a.name, b.name)),
    series: [...ix.series.values()].filter(s => named(s.name)).sort((a, b) => byName(a.name, b.name)),
  };
}

let anim = { entrance: true };
let closing = false;
const scrolls = {};
const topEntry = () => state.nav[state.nav.length - 1];
const keyOf = entry => `${entry.screen}:${entry.key ?? entry.id ?? ''}`;
const selecting = () => !!state.selection && state.selection.at === keyOf(topEntry());
const isSelected = b => selecting() && state.selection.ids.has(b.id);
const isDark = () => document.documentElement.dataset.theme === 'dark';

function applyTheme() {
  const { theme } = state.prefs;
  document.documentElement.dataset.theme = theme === 'dark' || (theme === 'system' && darkQuery.matches) ? 'dark' : 'light';
}

function render() {
  applyTheme();
  root.style.setProperty('--cols', Math.max(2, Math.round((root.clientWidth - 32) / 137)));
  root.querySelectorAll('[data-scroll]').forEach(el => {
    scrolls[el.dataset.scroll] = el.scrollTop;
  });
  const active = document.activeElement;
  const focus = active && root.contains(active) && active.id ? { id: active.id, start: active.selectionStart, end: active.selectionEnd } : null;
  root.innerHTML = renderScreen() + renderToolbar() + renderMenu() + renderSheet() + renderSnackbar();
  root.querySelectorAll('[data-scroll]').forEach(el => {
    el.scrollTop = scrolls[el.dataset.scroll] ?? 0;
  });
  const focusId = anim.focus ?? focus?.id;
  const el = focusId && document.getElementById(focusId);
  if (el) {
    el.focus({ preventScroll: true });
    if (focus && el.id === focus.id && focus.start != null) el.setSelectionRange?.(focus.start, focus.end);
  }
  renderNotes();
  anim = {};
}

function push(entry) {
  state.selection = null;
  state.menu = null;
  state.snackbar = null;
  state.nav.push(entry);
  delete scrolls[keyOf(entry)];
  anim.screen = 'forward';
  render();
}

function pop() {
  if (state.nav.length < 2) return;
  state.selection = null;
  state.menu = null;
  state.snackbar = null;
  state.nav.pop();
  anim.screen = 'back';
  render();
}

function openSheet(sheet) {
  state.menu = null;
  state.snackbar = null;
  state.sheet = sheet;
  delete scrolls[`sheet:${sheet.type}`];
  anim.sheet = true;
  anim.focus = { bulk: `f-${sheet.field}`, rename: 'f-name' }[sheet.type];
  render();
}

function closeSheet(then) {
  if (!state.sheet || closing) return;
  const finish = () => {
    closing = false;
    state.sheet = null;
    if (then) then();
    else render();
  };
  if (reduceMotion.matches) return finish();
  closing = true;
  root.querySelector('.sheet')?.classList.add('closing');
  root.querySelector('.scrim')?.classList.add('closing');
  setTimeout(finish, 180);
}

function snack(text, undo) {
  const id = Date.now() + Math.random();
  state.snackbar = { id, text, undo };
  anim.snack = true;
  render();
  setTimeout(() => {
    if (state.snackbar?.id !== id) return;
    state.snackbar = null;
    render();
  }, 6000);
}

function done(task) {
  if (state.tasks.has(task)) return;
  state.tasks.add(task);
  anim.task = task;
}

function greeting() {
  const hour = new Date().getHours();
  if (hour >= 5 && hour < 12) return 'Good morning';
  if (hour >= 12 && hour < 17) return 'Good afternoon';
  if (hour >= 17 && hour < 22) return 'Good evening';
  return 'Good night';
}

function renderScreen() {
  const t = topEntry();
  const renderers = { library: renderLibrary, series: renderSeries, author: renderAuthor, search: renderSearch, player: renderPlayer, preview: renderPreview };
  const style = t.screen === 'player' ? `style="${coverVars(bookById(t.id))}"` : '';
  return `<section class="screen ${t.screen} ${anim.screen ? `enter-${anim.screen}` : ''}" ${style}>${renderers[t.screen](t)}</section>`;
}

const gone = text => `<div class="topbar"><button class="icon-btn" data-action="back" aria-label="Back">${icon('back')}</button></div><p class="empty">${text}</p>`;
const bar = (progress, wavy = false) => `<div class="bar ${wavy ? 'wavy' : ''}" style="--w:${(Math.min(1, Math.max(0, progress)) * 100).toFixed(1)}%"><i></i></div>`;
const metaRow = (progress, text) => `<div class="row-meta">${progress != null ? bar(progress) : ''}<span>${text}</span></div>`;
const hiddenChip = '<span class="tag-chip">Hidden</span>';

function selectionBar() {
  return `<div class="topbar selbar"><button class="icon-btn" data-action="sel-close" aria-label="Stop selecting">${icon('close')}</button><div class="selbar-title">${state.selection.ids.size} selected</div><button class="text-btn" data-action="sel-all">Select all</button></div>`;
}

function unitAttrs(u) {
  const ids = `data-ids="${u.books.map(b => b.id).join(',')}" role="button" tabindex="0"`;
  if (u.kind === 'book') return `data-action="open-book" data-id="${u.book.id}" data-long="book" ${ids}`;
  return `data-action="open-${u.kind}" data-key="${esc(u.key)}" data-long="${u.kind}" ${ids}`;
}

function thumbHtml(u, { cls = '', count = false } = {}) {
  const { books } = u;
  const selected = selecting() && books.every(isSelected);
  const finished = books.every(b => b.finished);
  let inner;
  let kind = '';
  if (u.kind === 'book') {
    inner = cover(u.book);
  } else if (u.kind === 'series') {
    kind = 'stack';
    const next = nextOf(books);
    const rest = books.filter(b => b !== next);
    const layer = (b, n) => `<div class="layer l${n}" style="background:${b ? coverPalette(b).c1 : 'var(--surface-highest)'}"></div>`;
    inner = layer(rest[1] ?? rest[0], 2) + layer(rest[0], 1) + cover(next);
  } else {
    kind = 'mosaic';
    const picks = [...u.units.map(x => (x.kind === 'series' ? nextOf(x.books) : x.book))];
    for (const b of books) if (picks.length < 4 && !picks.includes(b)) picks.push(b);
    inner = picks.slice(0, 4).map(cover).join('') + '<div class="cover blank"></div>'.repeat(Math.max(0, 4 - picks.length));
  }
  const countBadge = count && u.kind !== 'book' ? `<span class="badge count">${finished ? icon('check') : ''}${books.length}</span>` : '';
  let corner = '';
  if (books.every(b => b.hidden)) corner = `<span class="badge corner quiet">${icon('hidden')}</span>`;
  else if (finished && !countBadge) corner = `<span class="badge corner done">${icon('check')}</span>`;
  const check = selected ? `<span class="badge picked">${icon('check')}</span>` : '';
  return `<div class="thumb ${kind} ${cls} ${selected ? 'is-picked' : ''}">${inner}${countBadge}${corner}${check}</div>`;
}

function unitInfo(u) {
  const ix = getIndex();
  if (u.kind === 'book') {
    const b = u.book;
    const left = `${fmtTime(b.duration - b.position)} left`;
    return {
      name: titleOf(b),
      sub: [authorOf(b), seriesLine(b)].filter(Boolean).join(' · '),
      meta: b.finished ? '' : metaRow(b.position > 0 ? b.position / b.duration : null, left),
    };
  }
  const n = u.books.length;
  const finished = u.books.filter(b => b.finished).length;
  if (u.kind === 'series') {
    const s = ix.series.get(u.key);
    const started = u.books.some(b => b.finished || b.position > 0);
    return {
      name: s.name,
      sub: ix.authors.get(s.authorKey)?.name ?? '',
      meta: started && finished < n ? metaRow(seriesProgress(u.books), finished ? `${finished} of ${n} finished` : 'In progress') : metaRow(null, plural(n, 'book')),
    };
  }
  const seriesCount = u.units.filter(x => x.kind === 'series').length;
  return {
    name: ix.authors.get(u.key).name,
    sub: [seriesCount && plural(seriesCount, 'series', 'series'), plural(n, 'book'), finished && `${finished} finished`].filter(Boolean).join(' · '),
    meta: '',
  };
}

function rowHtml(u, { sub, number, trailing = '' } = {}) {
  const info = unitInfo(u);
  const selected = selecting() && u.books.every(isSelected);
  const subText = sub ?? info.sub;
  const hidden = u.books.every(b => b.hidden) ? hiddenChip : '';
  return `<div class="row ${selected ? 'selected' : ''}" ${unitAttrs(u)}>
    ${number != null ? `<span class="part-num">${number}</span>` : ''}
    ${thumbHtml(u)}
    <div class="row-text"><div class="row-title book-text">${esc(info.name)}</div>${subText ? `<div class="row-sub book-text">${esc(subText)}</div>` : ''}${info.meta}</div>
    ${hidden}${trailing}
  </div>`;
}

function tileHtml(u) {
  const info = unitInfo(u);
  const selected = selecting() && u.books.every(isSelected);
  let progress = null;
  if (u.kind === 'book' && !u.book.finished && u.book.position > 0) progress = u.book.position / u.book.duration;
  if (u.kind === 'series' && u.books.some(b => b.finished || b.position > 0) && !allFinished(u)) progress = seriesProgress(u.books);
  const sub = u.kind === 'book' ? authorOf(u.book) ?? '' : info.sub;
  return `<div class="tile ${selected ? 'selected' : ''}" ${unitAttrs(u)}>${thumbHtml(u, { count: true })}${progress != null ? bar(progress) : ''}<div class="tile-title book-text">${esc(info.name)}</div><div class="tile-sub book-text">${esc(sub)}</div></div>`;
}

function libraryTopBar() {
  const p = state.prefs;
  const filtered = !p.showFinished || p.off.size > 0;
  return `<div class="topbar">
    <button class="search-pill" data-action="search">${icon('search')}<span>Search your library</span></button>
    <button class="icon-btn" data-action="sort" aria-label="Sort and show${filtered ? ', a filter is on' : ''}">${icon('sort')}${filtered ? '<span class="dot"></span>' : ''}</button>
    <button class="icon-btn" data-action="settings" aria-label="Settings">${icon('settings')}</button>
  </div>`;
}

function heroHtml({ book: b, upNext }) {
  const playing = state.playing && state.currentBookId === b.id;
  const series = seriesLine(b);
  const progress = b.position / b.duration;
  return `<div class="hero span" style="${coverVars(b)}" data-action="open-book" data-id="${b.id}" data-ids="${b.id}" data-long="book" role="button" tabindex="0">
    <div class="hero-top">${thumbHtml(bookUnit(b), { cls: 'hero-thumb' })}
      <div class="hero-text"><div class="hero-label">${upNext ? 'Up next' : 'Continue listening'}</div><div class="hero-title book-text">${esc(titleOf(b))}</div>${authorOf(b) ? `<div class="hero-sub book-text">${esc(authorOf(b))}</div>` : ''}${series ? `<div class="hero-series book-text">${esc(series)}</div>` : ''}</div>
    </div>
    <div class="hero-bottom">
      <div class="hero-progress">${bar(progress, playing)}<div class="hero-times"><span class="pct">${percent(progress)} of book</span><span class="left">${fmtTime(b.duration - b.position)} left</span></div></div>
      ${playButton(playing, 'hero-play', 'play-hero')}
    </div>
  </div>`;
}

function renderLibrary(t) {
  const m = libraryModel();
  const grid = state.prefs.layout === 'grid';
  const inProgress = state.books.filter(b => inLibrary(b) && statusOf(b) === 'current').length;
  const parts = [`<div class="greeting span"><h1>${greeting()}</h1>${inProgress ? `<p>${plural(inProgress, 'book')} in progress</p>` : ''}</div>`];
  if (m.hero) parts.push(heroHtml(m.hero));
  let shown = 0;
  for (const section of m.sections) {
    const count = section.groups ? section.count : section.items.length;
    if (!count) continue;
    shown++;
    parts.push(`<h2 class="section-header span"><span>${section.title}</span><span class="count">${count.toLocaleString('en')}</span></h2>`);
    for (const group of section.groups ?? [{ items: section.items }]) {
      if (group.label) parts.push(`<h3 class="group-label span">${esc(group.label)}</h3>`);
      parts.push(grid ? group.items.map(u => tileHtml(u)).join('') : `<div class="rows span">${group.items.map(u => rowHtml(u)).join('')}</div>`);
    }
  }
  if (!shown && !m.hero) parts.push('<p class="empty span">No books to show. The button next to search decides what the library shows.</p>');
  return `${selecting() ? selectionBar() : libraryTopBar()}<div class="scroll" data-scroll="${keyOf(t)}"><div class="lib ${grid ? 'grid' : 'list'} ${anim.entrance ? 'entering' : ''}">${parts.join('')}</div></div>`;
}

function moreButton() {
  return `<button class="icon-btn" data-action="menu" aria-label="More options">${icon('more')}</button>`;
}

function renderSeries(t) {
  const ix = getIndex();
  const s = ix.series.get(t.key);
  if (!s) return gone('This series is gone.');
  const finished = s.books.filter(b => b.finished).length;
  const author = ix.authors.get(s.authorKey);
  const cta = seriesCta(s);
  const rows = s.books.map((b, i) => {
    const status = statusOf(b);
    const sub = status === 'finished' ? 'Finished' : status === 'current' ? `${percent(b.position / b.duration)} · ${fmtTime(b.duration - b.position)} left` : fmtTime(b.duration);
    const selected = isSelected(b);
    return `<div class="row ${selected ? 'selected' : ''}" ${unitAttrs(bookUnit(b))}>
      <span class="part-num">${partOf(b) ?? i + 1}</span>${thumbHtml(bookUnit(b), { cls: 'small' })}
      <div class="row-text"><div class="row-title book-text">${esc(titleOf(b))}</div><div class="row-sub">${sub}</div>${status === 'current' ? bar(b.position / b.duration) : ''}</div>
      ${b.hidden ? hiddenChip : ''}${status === 'finished' ? icon('check', 'ic trailing') : ''}
    </div>`;
  }).join('');
  return `${selecting() ? selectionBar() : `<div class="topbar"><button class="icon-btn" data-action="back" aria-label="Back">${icon('back')}</button><div class="grow"></div>${moreButton()}</div>`}
  <div class="scroll" data-scroll="${keyOf(t)}">
    <div class="screen-head series-head">${thumbHtml(seriesUnit(s.key, s.books), { cls: 'big' })}
      <div><h1 class="head-title book-text">${esc(s.name)}</h1>${author ? `<button class="link book-text" data-action="go-author" data-key="${esc(author.key)}">${esc(author.name)}</button>` : ''}<div class="head-stats">${plural(s.books.length, 'book')} · ${finished} finished</div></div>
    </div>
    ${cta && !selecting() ? `<div class="cta"><button class="filled wide" data-action="series-play" data-id="${cta.book.id}">${icon('play')}${cta.label}</button></div>` : '<div class="cta-gap"></div>'}
    <div class="rows pad">${rows}</div>
  </div>`;
}

function renderAuthor(t) {
  const m = authorModel(t.key);
  if (!m) return gone('This author is gone.');
  const finished = m.books.filter(b => b.finished).length;
  const stats = [m.stacks.length && plural(m.stacks.length, 'series', 'series'), plural(m.books.length, 'book'), finished && `${finished} finished`].filter(Boolean).join(' · ');
  const rows = [
    ...m.stacks.map(u => rowHtml(u, { sub: plural(u.books.length, 'book') })),
    ...m.others.map(b => rowHtml(bookUnit(b), { sub: seriesLine(b) ?? '' })),
  ].join('');
  return `${selecting() ? selectionBar() : `<div class="topbar"><button class="icon-btn" data-action="back" aria-label="Back">${icon('back')}</button><div class="grow"></div>${moreButton()}</div>`}
  <div class="scroll" data-scroll="${keyOf(t)}">
    <div class="screen-head author-head"><h1 class="head-title big book-text">${esc(m.name)}</h1><div class="head-stats">${stats}</div></div>
    <div class="rows pad">${rows}</div>
  </div>`;
}

function renderSearch(t) {
  const q = state.search.query;
  const field = `<div class="search-top"><div class="search-field">
      <button class="icon-btn" data-action="back" aria-label="Back">${icon('back')}</button>
      <input id="q" type="search" placeholder="Search your library" value="${esc(q)}" autocomplete="off" spellcheck="false" enterkeyhint="search" aria-label="Search your library">
      <button class="icon-btn ${q ? '' : 'gone'}" id="q-clear" data-action="search-clear" aria-label="Clear">${icon('close')}</button>
    </div></div>`;
  return `${selecting() ? selectionBar() : field}<div class="scroll" data-scroll="${keyOf(t)}"><div class="search-body" id="search-body">${searchBody()}</div></div>`;
}

function searchBody() {
  const q = state.search.query;
  const model = searchModel(q);
  if (!model) return browseHtml();
  if (!model.books.length && !model.authors.length && !model.series.length) return `<p class="empty">Nothing found for “${esc(q.trim())}”.</p>`;
  const filter = state.search.filter;
  const fields = [['title', 'Titles'], ['author', 'Authors'], ['series', 'Series'], ['narrator', 'Narrators'], ['genre', 'Genres']]
    .map(([key, label]) => [key, label, model.books.filter(r => r.fields.has(key)).length])
    .filter(([, , n]) => n > 0);
  const chips = [['all', 'All', model.books.length], ...fields].map(([key, label, n]) => chip(`${label} ${n}`, filter === key, 'search-filter', key, 'radio')).join('');
  const results = filter === 'all' ? model.books : model.books.filter(r => r.fields.has(filter));
  const [first, ...rest] = results;
  const named = filter === 'all' ? [...model.authors.map(authorResultRow), ...model.series.map(seriesResultRow)] : [];
  return `<div class="chips scroll-x" role="radiogroup">${chips}</div>
    ${first ? topResultHtml(first) : ''}
    ${named.length ? `<div class="rows">${named.join('')}</div>` : ''}
    ${rest.length ? `<div class="rows">${rest.map(searchRow).join('')}</div>` : ''}`;
}

function matchTag(r) {
  const b = r.book;
  if (r.fields.has('narrator')) return `Narrator · ${b.narrator}`;
  if (r.fields.has('genre')) return `Genre · ${b.genre}`;
  if (seriesLine(b)) return `Series · ${seriesLine(b)}`;
  return '';
}

function topResultHtml(r) {
  const b = r.book;
  const playing = state.playing && state.currentBookId === b.id;
  const tag = matchTag(r);
  return `<div class="top-result ${isSelected(b) ? 'selected' : ''}" style="${coverVars(b)}" ${unitAttrs(bookUnit(b))}>
    ${thumbHtml(bookUnit(b), { cls: 'tr-thumb' })}
    <div class="tr-text">
      <div class="tr-title book-text">${esc(titleOf(b))}</div>
      ${authorOf(b) ? `<div class="tr-sub book-text">${esc(authorOf(b))}</div>` : ''}
      ${tag ? `<div class="tr-tag">${esc(tag)}</div>` : ''}
      <div class="tr-bottom"><div class="tr-progress">${!b.finished && b.position > 0 ? bar(b.position / b.duration) : ''}<span>${b.finished ? 'Finished' : `${fmtTime(b.duration - b.position)} left`}</span></div>${b.hidden ? hiddenChip : ''}${playButton(playing, 'small', 'search-play', `data-id="${b.id}"`)}</div>
    </div>
  </div>`;
}

function searchRow(r) {
  const b = r.book;
  const tag = matchTag(r);
  return `<div class="row ${isSelected(b) ? 'selected' : ''}" ${unitAttrs(bookUnit(b))}>
    ${thumbHtml(bookUnit(b), { cls: 'small' })}
    <div class="row-text"><div class="row-title book-text">${esc(titleOf(b))}</div><div class="row-sub book-text">${esc(authorOf(b) ?? '')}</div>${tag ? `<div class="row-tag">${esc(tag)}</div>` : ''}</div>
    ${b.hidden ? hiddenChip : ''}
  </div>`;
}

function authorResultRow(a) {
  return `<div class="row" data-action="go-author" data-key="${esc(a.key)}" role="button" tabindex="0">
    <span class="avatar">${shaped('cookie9', 'primary-container', 'on-primary-container', 'person')}</span>
    <div class="row-text"><div class="row-title book-text">${esc(a.name)}</div><div class="row-sub">Author · ${plural(a.books.length, 'book')}</div></div>${icon('chevron', 'ic trailing')}
  </div>`;
}

function seriesResultRow(s) {
  return `<div class="row" data-action="go-series" data-key="${esc(s.key)}" role="button" tabindex="0">
    ${thumbHtml(seriesUnit(s.key, s.books), { cls: 'small' })}
    <div class="row-text"><div class="row-title book-text">${esc(s.name)}</div><div class="row-sub">Series · ${plural(s.books.length, 'book')}</div></div>${icon('chevron', 'ic trailing')}
  </div>`;
}

const BROWSE_SHAPES = ['cookie9', 'clover', 'sunny', 'flower'];
const BROWSE_COLORS = [['primary-container', 'on-primary-container'], ['tertiary-container', 'on-tertiary-container'], ['secondary-container', 'on-secondary-container']];

function browseHtml() {
  const tab = state.search.browse;
  const ix = getIndex();
  let entries;
  if (tab === 'authors') {
    entries = [...ix.authors.values()].map(a => ({ name: a.name, books: a.books, attrs: `data-action="go-author" data-key="${esc(a.key)}"` }));
  } else if (tab === 'series') {
    entries = [...ix.series.values()].map(s => ({ name: s.name, books: s.books, attrs: `data-action="go-series" data-key="${esc(s.key)}"` }));
  } else {
    const field = tab === 'narrators' ? 'narrator' : 'genre';
    const byKey = new Map();
    for (const b of state.books) {
      const names = field === 'genre' ? (b.genre ?? '').split(',').map(g => g.trim()).filter(Boolean) : b.narrator ? [b.narrator] : [];
      for (const name of names) {
        if (!byKey.has(norm(name))) byKey.set(norm(name), { name, books: [] });
        byKey.get(norm(name)).books.push(b);
      }
    }
    entries = [...byKey.values()].map(e => ({ ...e, attrs: `data-action="browse-name" data-field="${field}" data-value="${esc(e.name)}"` }));
  }
  entries.sort((a, b) => byName(a.name, b.name));
  const tabs = [['authors', 'Authors'], ['series', 'Series'], ['narrators', 'Narrators'], ['genres', 'Genres']];
  const rows = entries.map((e, i) => {
    const inProgress = e.books.filter(b => statusOf(b) === 'current').length;
    const finished = e.books.filter(b => b.finished).length;
    const summary = [plural(e.books.length, 'book'), inProgress && `${inProgress} in progress`, finished && `${finished} finished`].filter(Boolean).join(' · ');
    const initials = e.name.split(/\s+/).filter(w => /^[\p{L}\p{N}]/u.test(w)).slice(0, 2).map(w => w[0].toUpperCase()).join('');
    const [bg, fg] = BROWSE_COLORS[i % 3];
    return `<div class="row" ${e.attrs} role="button" tabindex="0"><span class="avatar">${shaped(BROWSE_SHAPES[i % 4], bg, fg, initials)}</span><div class="row-text"><div class="row-title book-text">${esc(e.name)}</div><div class="row-sub">${summary}</div></div>${icon('chevron', 'ic trailing')}</div>`;
  }).join('');
  return `<h2 class="browse-title">Browse</h2><div class="chips scroll-x" role="radiogroup">${tabs.map(([key, label]) => chip(label, tab === key, 'browse-tab', key, 'radio')).join('')}</div><div class="rows">${rows}</div>`;
}

function renderPlayer(t) {
  const b = bookById(t.id);
  if (!b) return gone('This book is gone.');
  const playing = state.playing && state.currentBookId === b.id;
  const aKey = authorKey(b);
  const sKey = seriesKey(b);
  const next = b.finished ? nextInSeries(b) : null;
  const links = [
    aKey && `<button class="link" data-action="go-author" data-key="${esc(aKey)}">${esc(getIndex().authors.get(aKey).name)}</button>`,
    sKey && `<button class="link" data-action="go-series" data-key="${esc(sKey)}">${esc(seriesLine(b))}</button>`,
  ].filter(Boolean).join('<span class="sep">·</span>');
  return `<div class="topbar"><button class="icon-btn" data-action="back" aria-label="Close player">${icon('down')}</button></div>
  <div class="scroll" data-scroll="${keyOf(t)}"><div class="player-body">
    ${thumbHtml(bookUnit(b), { cls: 'player-thumb' })}
    <h1 class="player-title book-text">${esc(titleOf(b))}</h1>
    <div class="player-links book-text">${links}</div>
    <input class="seek" type="range" min="0" max="${b.duration}" step="1" value="${Math.round(b.position)}" data-seek="${b.id}" aria-label="Position" style="--p:${((b.position / b.duration) * 100).toFixed(2)}%">
    <div class="times" id="times">${timesHtml(b)}</div>
    ${next ? upNextHtml(next) : ''}
    <div class="controls">
      <button class="ctrl" data-action="player-skip" data-value="-30" aria-label="Back 30 seconds">${icon('replay')}</button>
      ${playButton(playing, 'big', 'player-play', `data-id="${b.id}"`)}
      <button class="ctrl" data-action="player-skip" data-value="30" aria-label="Forward 30 seconds">${icon('replay', 'ic flip')}</button>
    </div>
    ${b.finished ? '' : `<button class="proto-chip" data-action="player-end" data-id="${b.id}">Prototype: skip to the end</button>`}
  </div></div>`;
}

const timesHtml = b => `<span>${percent(b.position / b.duration)} of book</span><span>${b.finished ? 'Finished' : `${fmtTime(b.duration - b.position)} left`}</span>`;

function upNextHtml(next) {
  return `<div class="upnext" data-action="up-next" data-id="${next.id}" role="button" tabindex="0" style="${coverVars(next)}">
    ${thumbHtml(bookUnit(next), { cls: 'small' })}
    <div class="row-text"><div class="upnext-label">Up next</div><div class="row-title book-text">${esc(titleOf(next))}</div><div class="row-sub book-text">${esc(seriesLine(next))}</div></div>
    ${playButton(false, 'small', 'up-next', `data-id="${next.id}"`)}
  </div>`;
}

function previewCount() {
  return PREVIEW.items.reduce((n, item) => n + (state.preview.choices[item.key] === 'series' ? item.files.length : 1), 0);
}

function renderPreview(t) {
  const count = previewCount();
  const rows = PREVIEW.items.map((item, i) => {
    const series = state.preview.choices[item.key] === 'series';
    const hours = Math.round(item.files.reduce((sum, f) => sum + f.duration, 0) / 3600);
    const sub = series
      ? ['Series', item.author, plural(item.files.length, 'book')].filter(Boolean).join(' · ')
      : [item.author, `${hours} hr`, plural(item.files.length, 'audio file')].filter(Boolean).join(' · ');
    const [bg, fg] = BROWSE_COLORS[i % 3];
    const attrs = item.single ? '' : `data-action="preview-item" data-key="${item.key}" role="button" tabindex="0"`;
    return `<div class="row ${item.single ? 'static' : ''}" ${attrs}><span class="avatar">${shaped(series ? 'clover' : 'cookie6', bg, fg, item.title.replace(/^The /, '')[0])}</span><div class="row-text"><div class="row-title book-text">${esc(item.title)}</div><div class="row-sub">${esc(sub)}</div></div>${item.single ? '' : icon('chevron', 'ic trailing')}</div>`;
  }).join('');
  return `<div class="topbar"><button class="icon-btn" data-action="back" aria-label="Back">${icon('back')}</button></div>
  <div class="scroll" data-scroll="${keyOf(t)}"><div class="preview-head"><h1>We found ${plural(count, 'book')}</h1><p>Tap one to split or join it.</p></div><div class="rows pad">${rows}</div></div>
  <div class="bottom-bar"><button class="filled wide" data-action="preview-add">Add ${plural(count, 'book')}</button></div>`;
}

function renderToolbar() {
  if (!selecting()) return '';
  const disabled = state.selection.ids.size ? '' : 'disabled';
  const button = (action, label, name) => `<button data-action="${action}" ${disabled}>${icon(name)}${label}</button>`;
  return `<div class="toolbar" role="toolbar" aria-label="Change the selected books">${button('sel-series', 'Series', 'stack')}${button('sel-author', 'Author', 'person')}${button('sel-hide', 'Hide', 'hidden')}</div>`;
}

function renderSnackbar() {
  const s = state.snackbar;
  if (!s) return '';
  return `<div class="snackbar ${selecting() ? 'raised' : ''} ${anim.snack ? 'enter' : ''}" role="status"><span>${esc(s.text)}</span>${s.undo ? '<button data-action="snack-undo">Undo</button>' : ''}</div>`;
}

function menuItems() {
  const { kind, key } = state.menu;
  const ix = getIndex();
  const books = kind === 'series' ? ix.series.get(key)?.books ?? [] : authorModel(key)?.books ?? [];
  const allHidden = books.length && books.every(b => b.hidden);
  const items = [];
  if (kind === 'series' || books.some(b => !isServer(b))) items.push(['rename', 'Rename', 'edit']);
  items.push(allHidden ? ['unhide-stack', 'Show in library', 'visible'] : ['hide-stack', kind === 'series' ? 'Hide' : 'Hide all their books', 'hidden']);
  items.push(['select-stack', 'Select', 'select']);
  if (kind === 'series' && joinable(key)) items.push(['join', 'Join into one book', 'merge']);
  return items;
}

function renderMenu() {
  if (!state.menu) return '';
  const { kind, key } = state.menu;
  return `<div class="menu-scrim" data-action="menu-close"></div><div class="menu" role="menu">${menuItems().map(([action, label, name]) => `<button role="menuitem" data-action="${action}" data-kind="${kind}" data-key="${esc(key)}">${icon(name)}${label}</button>`).join('')}</div>`;
}

function chip(label, on, action, val, role = 'checkbox', lead) {
  return `<button class="chip ${on ? 'on' : ''}" data-action="${action}" data-value="${esc(val)}" role="${role}" aria-checked="${on}">${on ? icon('check') : lead ? icon(lead) : ''}${esc(label)}</button>`;
}

function switchRow(action, title, sub, on) {
  return `<button class="switch-row ${on ? 'on' : ''}" data-action="${action}" role="switch" aria-checked="${on}"><span class="grow"><span class="sr-title">${title}</span>${sub ? `<span class="sr-sub">${sub}</span>` : ''}</span><span class="switch"></span></button>`;
}

function segmented(action, options, current) {
  return `<div class="segmented" role="radiogroup">${options.map(([v, label, name]) => `<button class="${current === v ? 'on' : ''}" data-action="${action}" data-value="${v}" role="radio" aria-checked="${current === v}">${current === v ? icon('check') : name ? icon(name) : ''}${label}</button>`).join('')}</div>`;
}

function actionRow(action, label, shapeName, bg, fg, name, attrs = '', sub = '') {
  return `<button class="action" data-action="${action}" ${attrs}>${shaped(shapeName, bg, fg, name)}<span class="grow">${label}${sub ? `<span class="sr-sub">${sub}</span>` : ''}</span></button>`;
}

function sheetHead(u, title, lines) {
  return `<div class="sheet-head">${thumbHtml(u)}<div class="sheet-head-text"><div class="sheet-head-title book-text">${esc(title)}</div>${lines}</div></div>`;
}

const SHEETS = {
  sort() {
    const p = state.prefs;
    const hiddenCount = state.books.filter(b => b.hidden).length;
    const sorts = [['progress', 'Progress'], ['title', 'Title'], ['author', 'Author'], ['recent', 'Recently added']];
    return `<h3 class="label">Sort by</h3><div class="chips" role="radiogroup">${sorts.map(([k, l]) => chip(l, p.sort === k, 'sort-by', k, 'radio')).join('')}</div>
      <h3 class="label">Show</h3>${switchRow('toggle-finished', 'Finished books', null, p.showFinished)}${switchRow('toggle-hidden', 'Hidden books', plural(hiddenCount, 'book'), p.showHidden)}
      ${state.sources.length > 1 ? `<h3 class="label">From</h3><div class="chips">${state.sources.map(s => chip(s.name, !p.off.has(s.id), 'toggle-source', s.id, 'checkbox', s.server ? 'dns' : 'folder')).join('')}</div>` : ''}`;
  },

  settings() {
    const p = state.prefs;
    const added = state.sources.some(s => s.id === PREVIEW.source.id);
    const folders = state.sources.map(s => `<div class="action static">${shaped(s.server ? 'cookie9' : 'cookie4', s.server ? 'tertiary-container' : 'primary-container', s.server ? 'on-tertiary-container' : 'on-primary-container', s.server ? 'dns' : 'folder')}<span class="grow">${esc(s.name)}<span class="sr-sub">${plural(state.books.filter(b => b.source === s.id).length, 'book')}</span></span></div>`).join('');
    return `<h2 class="sheet-title">Prototype settings</h2><p class="hint">These aren't part of the plan.</p>
      <h3 class="label">Layout</h3>${segmented('layout', [['list', 'List', 'list'], ['grid', 'Grid', 'grid']], p.layout)}
      <h3 class="label">Theme</h3>${segmented('theme', [['system', 'System'], ['light', 'Light'], ['dark', 'Dark']], p.theme)}
      <h3 class="label">Audiobook folders</h3><div class="actions">${folders}${added ? '' : actionRow('add-folder', 'Add a folder', 'sunny', 'secondary-container', 'on-secondary-container', 'add', '', 'See what Voice finds in it')}</div>
      <div class="sheet-actions"><button class="text-btn" data-action="reset">${icon('restart')}Start over</button></div>`;
  },

  book({ id }) {
    const b = bookById(id);
    if (!b) return '';
    const ix = getIndex();
    const aKey = authorKey(b);
    const sKey = seriesKey(b);
    const links = [
      aKey && `<button class="link" data-action="go-author" data-key="${esc(aKey)}">${esc(ix.authors.get(aKey).name)}</button>`,
      sKey && `<button class="link" data-action="go-series" data-key="${esc(sKey)}">${esc(seriesLine(b))}</button>`,
    ].filter(Boolean).join('<span class="sep">·</span>');
    const source = sourceOf(b);
    const progress = b.finished ? 'Finished' : `${percent(b.position / b.duration)} of book · ${fmtTime(b.duration - b.position)} left`;
    const status = statusOf(b);
    const tiles = [
      ['notStarted', 'Not started yet', 'cookie6', 'hourglass', 'secondary-container', 'on-secondary-container'],
      ['current', 'Current', 'sunny', 'headphones', 'primary-container', 'on-primary-container'],
      ['finished', 'Completed', 'burst', 'check', 'tertiary-container', 'on-tertiary-container'],
    ].map(([v, label, shapeName, name, bg, fg]) => {
      const on = status === v;
      return `<button class="status ${on ? 'on' : ''}" data-action="status" data-value="${v}" data-id="${b.id}" role="radio" aria-checked="${on}">${on ? shaped(shapeName, bg, fg, name) : shaped('circle', 'surface-highest', 'on-surface-variant', name)}<span>${label}</span></button>`;
    }).join('');
    const folder = b.folderKey && state.folders[b.folderKey];
    const attrs = `data-id="${b.id}"`;
    const actions = [
      actionRow('edit', 'Edit', 'pentagon', 'secondary-container', 'on-secondary-container', 'edit', attrs),
      actionRow('cover', 'Cover from the Internet', 'clover', 'tertiary-container', 'on-tertiary-container', 'public', attrs),
      actionRow('cover', 'Cover from the file system', 'cookie4', 'primary-container', 'on-primary-container', 'image', attrs),
      folder && folder.files.length > 1 ? actionRow('split', `Split into ${plural(folder.files.length, 'book')}`, 'flower', 'tertiary-container', 'on-tertiary-container', 'split', attrs) : '',
      b.hidden
        ? actionRow('unhide', 'Show in library', 'cookie6', 'secondary-container', 'on-secondary-container', 'visible', attrs)
        : actionRow('hide', 'Hide from library', 'cookie6', 'secondary-container', 'on-secondary-container', 'hidden', attrs),
      actionRow('select', 'Select', 'circle', 'surface-highest', 'on-surface-variant', 'select', attrs),
    ].join('');
    return `${sheetHead(bookUnit(b), titleOf(b), `${links ? `<div class="links book-text">${links}</div>` : ''}<div class="head-line">${progress}</div><div class="head-line">${icon(source.server ? 'dns' : 'phone')}${source.server ? esc(source.name) : `On this device · ${esc(source.name)}`}</div>`)}
      <div class="status-tiles" role="radiogroup">${tiles}</div>
      <div class="actions">${actions}</div>
      <div class="actions">${actionRow('delete', 'Delete book', 'cookie9', 'error-container', 'on-error-container', 'delete', `${attrs} data-danger`)}</div>`;
  },

  stack({ kind, key }) {
    const ix = getIndex();
    const isSeries = kind === 'series';
    const s = isSeries ? ix.series.get(key) : null;
    const m = isSeries ? null : authorModel(key);
    if (!s && !m) return '';
    const books = isSeries ? s.books : m.books;
    const unit = isSeries ? seriesUnit(key, books) : authorUnit(key, [...m.stacks, ...m.others.map(bookUnit)]);
    const finished = books.filter(b => b.finished).length;
    const line = isSeries
      ? ['Series', plural(books.length, 'book'), ix.authors.get(s.authorKey)?.name].filter(Boolean).join(' · ')
      : ['Author', m.stacks.length && plural(m.stacks.length, 'series', 'series'), plural(books.length, 'book'), finished && `${finished} finished`].filter(Boolean).join(' · ');
    const attrs = `data-kind="${kind}" data-key="${esc(key)}"`;
    const allHidden = books.every(b => b.hidden);
    const canRename = isSeries || books.some(b => !isServer(b));
    return `${sheetHead(unit, isSeries ? s.name : m.name, `<div class="head-line">${esc(line)}</div>`)}
      <div class="actions">
        ${canRename ? actionRow('rename', 'Rename', 'pentagon', 'secondary-container', 'on-secondary-container', 'edit', attrs) : ''}
        ${allHidden
          ? actionRow('unhide-stack', 'Show in library', 'cookie6', 'secondary-container', 'on-secondary-container', 'visible', attrs)
          : actionRow('hide-stack', isSeries ? 'Hide from library' : 'Hide all their books', 'cookie6', 'secondary-container', 'on-secondary-container', 'hidden', attrs)}
        ${actionRow('select-stack', 'Select', 'circle', 'surface-highest', 'on-surface-variant', 'select', attrs)}
        ${isSeries && joinable(key) ? actionRow('join', 'Join into one book', 'flower', 'tertiary-container', 'on-tertiary-container', 'merge', attrs) : ''}
      </div>`;
  },

  edit(s) {
    const b = bookById(s.id);
    const d = s.draft;
    return `${sheetHead(bookUnit(b), titleOf(b), `<div class="head-line book-text">${esc(authorOf(b) ?? '')}</div>`)}
      ${field('title', 'Title', d.title)}
      ${field('author', 'Author', d.author, true)}
      <div class="field-row">${field('series', 'Series', d.series, true)}${field('part', 'Part', d.part, false, 'numeric')}</div>
      <p class="hint">${isServer(b) ? `Changes go to ${esc(sourceOf(b).name)}.` : 'Your files stay as they are.'}</p>
      <div class="sheet-actions"><button class="text-btn" data-action="reset-edit" ${Object.keys(b.edit).length ? '' : 'disabled'}>Use the names from the files</button><button class="filled small" data-action="save-edit">Save</button></div>`;
  },

  rename(s) {
    const isSeries = s.kind === 'series';
    const entry = (isSeries ? getIndex().series : getIndex().authors).get(s.key);
    const left = isSeries ? 0 : entry.books.filter(isServer).length;
    return `<h2 class="sheet-title">Rename ${isSeries ? 'series' : 'author'}</h2>
      ${field('name', isSeries ? 'Series' : 'Author', s.draft, true)}
      <p class="hint">Picking a suggested name joins the two.${left ? ` Leaves out ${plural(left, 'book')} from Audiobookshelf.` : ''}</p>
      <div class="sheet-actions"><button class="text-btn" data-action="sheet-close">Cancel</button><button class="filled small" data-action="save-rename">Save</button></div>`;
  },

  bulk(s) {
    const books = s.ids.map(bookById).filter(Boolean);
    const target = books.filter(b => !isServer(b));
    const left = books.length - target.length;
    const isSeries = s.field === 'series';
    const notes = [isSeries && 'Their order follows their file names.', left && `Leaves out ${plural(left, 'book')} from Audiobookshelf.`].filter(Boolean).join(' ');
    return `<h2 class="sheet-title">${isSeries ? 'Series' : 'Author'} of ${plural(target.length, 'book')}</h2>
      ${field(s.field, isSeries ? 'Series' : 'Author', s.draft, true)}
      ${notes ? `<p class="hint">${notes}</p>` : ''}
      <div class="sheet-actions"><button class="text-btn" data-action="sheet-close">Cancel</button><button class="filled small" data-action="save-bulk" ${target.length ? '' : 'disabled'}>Save</button></div>`;
  },

  organize({ key }) {
    const item = PREVIEW.items.find(i => i.key === key);
    const choice = state.preview.choices[key];
    const names = item.files.slice(0, 3).map(f => f.name).join(', ') + (item.files.length > 3 ? ', …' : '');
    const option = (v, title, description, count) => `<button class="option ${choice === v ? 'on' : ''}" data-action="organize" data-value="${v}" data-key="${key}" role="radio" aria-checked="${choice === v}">
      <span class="radio"></span><span class="option-text">${item.guess === v ? '<span class="looks">Looks like yours</span>' : ''}<span class="option-title">${esc(title)}</span><span class="option-desc">${esc(description)}</span><span class="option-count">${count}</span></span></button>`;
    return `<h2 class="sheet-title center">How is ${esc(item.title)} organized?</h2><div role="radiogroup">
      ${option('book', `${item.title} is one book`, 'Its files become chapters, even in folders like CD 1 and CD 2.', '1 book')}
      ${option('series', `Every folder or file in ${item.title} is a book`, names, `${plural(item.files.length, 'book')}, a series`)}</div>`;
  },
};

function field(name, label, val, suggest = false, inputmode = '') {
  return `<div class="field f-${name}"><input id="f-${name}" data-field="${name}" value="${esc(val ?? '')}" autocomplete="off" spellcheck="false" placeholder=" " ${inputmode ? `inputmode="${inputmode}"` : ''}><label for="f-${name}">${label}</label>${suggest ? `<div class="suggest" id="s-${name}">${suggestions(name)}</div>` : ''}</div>`;
}

function suggestions(name) {
  const s = state.sheet;
  if (!s) return '';
  const kind = name === 'name' ? s.kind : name;
  const typed = (s.type === 'edit' ? s.draft[name] : s.draft) ?? '';
  if (s.type === 'edit' && s.active !== name) return '';
  const map = kind === 'series' ? getIndex().series : getIndex().authors;
  const key = norm(typed);
  let entries;
  if (s.type === 'rename') {
    const own = words(map.get(s.key)?.name).filter(w => w.length > 2 && w !== 'the');
    entries = [...map.values()].filter(e => e.key !== s.key && e.key !== key && (words(e.name).some(w => own.includes(w)) || (key && e.key.includes(key))));
  } else {
    if (!key) return '';
    entries = [...map.values()].filter(e => e.key !== key && e.key.includes(key));
  }
  const rows = entries.slice(0, 3).map(e => `<button data-action="pick" data-field="${name}" data-value="${esc(e.name)}"><span class="s-name book-text">${esc(e.name)}</span><span class="s-sub">${plural(e.books.length, 'book')}</span></button>`);
  if (s.type !== 'rename' && key && !map.has(key)) rows.push(`<div class="s-new">New ${kind} “${esc(typed.trim())}”</div>`);
  return rows.join('');
}

function renderSheet() {
  const s = state.sheet;
  if (!s) return '';
  const cls = `${anim.sheet ? 'enter' : ''} ${closing ? 'closing' : ''}`;
  const style = s.type === 'book' || s.type === 'edit' ? `style="${coverVars(bookById(s.id))}"` : '';
  return `<div class="scrim ${cls}" data-action="sheet-close"></div><div class="sheet sheet-${s.type} ${cls}" ${style} role="dialog" aria-modal="true"><div class="handle"></div><div class="sheet-body" data-scroll="sheet:${s.type}">${SHEETS[s.type](s)}</div></div>`;
}

const snapshot = books => books.map(b => ({ b, edit: { ...b.edit }, hidden: b.hidden }));

function restore(snap) {
  for (const { b, edit, hidden } of snap) {
    b.edit = edit;
    b.hidden = hidden;
  }
  changed();
}

function hide(books, text) {
  const snap = snapshot(books);
  books.forEach(b => {
    b.hidden = true;
  });
  changed();
  snack(text, () => {
    restore(snap);
    done('hide-undo');
  });
}

function start(b) {
  if (b.finished) {
    b.finished = false;
    b.position = 0;
  }
  if (b.position === 0) b.position = 1;
  b.lastListenedAt = Date.now();
  state.currentBookId = b.id;
  state.playing = true;
  changed();
}

function finish(b) {
  b.position = b.duration;
  b.finished = true;
  b.lastListenedAt = Date.now();
  state.currentBookId = b.id;
  state.playing = false;
  changed();
  render();
  confetti(root.querySelector('.player-thumb'));
}

function setStatus(b, status) {
  b.finished = status === 'finished';
  if (status === 'finished') b.position = b.duration;
  if (status === 'notStarted') b.position = 0;
  if (status === 'current' && (b.position === 0 || b.position >= b.duration)) b.position = 1;
  if (status !== 'notStarted') b.lastListenedAt = Date.now();
  changed();
}

function joinable(key) {
  const s = getIndex().series.get(key);
  const folder = s?.books[0]?.fromFolder;
  return !!folder && s.books.every(b => b.fromFolder === folder);
}

function booksFromFolder(key, folder, base) {
  return folder.files.map((file, i) => makeBook({
    ...base,
    id: `${key}-${i + 1}`,
    title: file.title,
    author: folder.author,
    series: folder.title,
    file: file.name,
    duration: file.duration,
    hue: folder.hue + i * 4,
    light: folder.light,
    motif: folder.splitMotif ?? folder.motif,
    fromFolder: key,
  }));
}

function bookFromFolder(key, folder, base) {
  return makeBook({
    ...base,
    id: key,
    title: folder.title,
    author: folder.author,
    file: folder.title,
    duration: folder.files.reduce((sum, f) => sum + f.duration, 0),
    hue: folder.hue,
    light: folder.light,
    motif: folder.motif,
    folderKey: key,
  });
}

function replaceBooks(old, fresh) {
  const at = state.books.indexOf(old[0]);
  state.books = state.books.filter(b => !old.includes(b));
  state.books.splice(Math.max(0, at), 0, ...fresh);
}

function split(b) {
  const key = b.folderKey;
  const books = booksFromFolder(key, state.folders[key], { source: b.source, addedAt: b.addedAt, hidden: b.hidden });
  let begin = 0;
  for (const nb of books) {
    const end = begin + nb.duration;
    // the books before the one that was playing are done, the one playing keeps the position
    if (b.finished || b.position >= end) {
      nb.finished = true;
      nb.position = nb.duration;
      nb.lastListenedAt = b.lastListenedAt;
    } else if (b.position > begin) {
      nb.position = b.position - begin;
      nb.lastListenedAt = b.lastListenedAt;
    }
    begin = end;
  }
  replaceBooks([b], books);
  if (state.currentBookId === b.id) state.currentBookId = (books.find(x => statusOf(x) === 'current') ?? books[0]).id;
  changed();
  done('split');
  state.nav = state.nav.filter(e => !(e.screen === 'player' && e.id === b.id));
  push({ screen: 'series', key: norm(state.folders[key].title) });
  snack(`Split into ${plural(books.length, 'book')}`);
}

function join(key) {
  const books = getIndex().series.get(key).books;
  const folderKey = books[0].fromFolder;
  let position = 0;
  for (const b of books) {
    if (b.finished) {
      position += b.duration;
      continue;
    }
    position += b.position;
    break;
  }
  const added = books.map(b => b.addedAt).filter(t => t != null);
  const joined = bookFromFolder(folderKey, state.folders[folderKey], {
    source: books[0].source,
    addedAt: added.length ? Math.min(...added) : null,
    position,
    finished: books.every(b => b.finished),
    lastListenedAt: Math.max(0, ...books.map(b => b.lastListenedAt ?? 0)) || null,
    hidden: books.every(b => b.hidden),
  });
  replaceBooks(books, [joined]);
  if (books.some(b => b.id === state.currentBookId)) state.currentBookId = joined.id;
  changed();
  state.nav = state.nav.filter(e => !(e.screen === 'series' && e.key === key));
  anim.screen = 'back';
  snack('Joined into one book');
}

function addPreview() {
  const now = Date.now();
  state.sources.push({ ...PREVIEW.source });
  for (const item of PREVIEW.items) {
    const folder = { title: item.title, author: item.author, hue: item.hue, motif: item.motif, splitMotif: item.key === 'waldgeister' ? 'episode' : item.motif, files: item.files };
    state.folders[item.key] = folder;
    const base = { source: PREVIEW.source.id, addedAt: now };
    if (state.preview.choices[item.key] === 'series') state.books.push(...booksFromFolder(item.key, folder, base));
    else state.books.push(bookFromFolder(item.key, folder, base));
  }
  const count = previewCount();
  changed();
  done('folder');
  state.preview = null;
  pop();
  snack(`Added ${plural(count, 'book')} from ${PREVIEW.source.name}`);
}

function confetti(anchor) {
  if (!anchor || reduceMotion.matches) return;
  const box = anchor.getBoundingClientRect();
  const frame = root.getBoundingClientRect();
  const colors = ['var(--primary)', 'var(--tertiary-container)', 'var(--primary-container)', '#f6c344', '#ef7a85'];
  for (let i = 0; i < 28; i++) {
    const piece = document.createElement('span');
    const angle = (Math.PI * 2 * i) / 28 + Math.random() * 0.3;
    const distance = 90 + Math.random() * 90;
    piece.className = 'confetti';
    piece.style.cssText = `left:${box.left - frame.left + box.width / 2}px;top:${box.top - frame.top + box.height / 2}px;background:${colors[i % colors.length]};--dx:${Math.cos(angle) * distance}px;--dy:${Math.sin(angle) * distance - 40}px;--r:${Math.random() * 720 - 360}deg`;
    root.appendChild(piece);
    setTimeout(() => piece.remove(), 1300);
  }
}

const ids = el => el.dataset.ids.split(',').filter(Boolean);

function toggleSelection(list) {
  const selected = state.selection.ids;
  const all = list.every(id => selected.has(id));
  list.forEach(id => (all ? selected.delete(id) : selected.add(id)));
  render();
}

function startSelecting(books) {
  state.selection = { at: keyOf(topEntry()), ids: new Set(books.map(b => b.id)) };
  render();
}

function shownBooks() {
  const t = topEntry();
  if (t.screen === 'series') return getIndex().series.get(t.key)?.books ?? [];
  if (t.screen === 'author') return authorModel(t.key)?.books ?? [];
  if (t.screen === 'search') {
    const model = searchModel(state.search.query);
    if (!model) return [];
    const { filter } = state.search;
    return model.books.filter(r => filter === 'all' || r.fields.has(filter)).map(r => r.book);
  }
  const m = libraryModel();
  const units = m.sections.flatMap(s => s.groups ? s.groups.flatMap(g => g.items) : s.items);
  return [...new Set([...(m.hero ? [m.hero.book] : []), ...units.flatMap(u => u.books)])];
}

function stackBooks(kind, key) {
  return kind === 'series' ? getIndex().series.get(key)?.books ?? [] : authorModel(key)?.books ?? [];
}

function stackName(kind, key) {
  return (kind === 'series' ? getIndex().series : getIndex().authors).get(key)?.name ?? '';
}

function goTo(entry) {
  if (state.sheet) closeSheet(() => push(entry));
  else push(entry);
}

const ACTIONS = {
  back: pop,
  search() {
    state.search = { query: '', filter: 'all', browse: state.search.browse };
    anim.focus = 'q';
    push({ screen: 'search' });
  },
  sort: () => openSheet({ type: 'sort' }),
  settings: () => openSheet({ type: 'settings' }),
  'sheet-close': () => closeSheet(),
  'menu': () => {
    const t = topEntry();
    state.menu = { kind: t.screen, key: t.key };
    render();
  },
  'menu-close': () => {
    state.menu = null;
    render();
  },

  'open-book'(d, el) {
    if (selecting()) return toggleSelection(ids(el));
    push({ screen: 'player', id: d.id });
  },
  'open-series'(d, el) {
    if (selecting()) return toggleSelection(ids(el));
    push({ screen: 'series', key: d.key });
  },
  'open-author'(d, el) {
    if (selecting()) return toggleSelection(ids(el));
    if (topEntry().screen === 'library' && state.prefs.sort === 'author' && d.key === 'lenamorgan') done('author-stack');
    push({ screen: 'author', key: d.key });
  },
  'go-author': d => goTo({ screen: 'author', key: d.key }),
  'go-series': d => goTo({ screen: 'series', key: d.key }),

  'play-hero'() {
    const hero = heroModel();
    if (!hero) return;
    anim.morph = true;
    if (state.playing && state.currentBookId === hero.book.id) state.playing = false;
    else {
      if (hero.upNext) done('next-book');
      start(hero.book);
    }
    render();
  },
  'series-play'(d) {
    const b = bookById(d.id);
    if (seriesKey(b) === 'thesignalsaga') done('series-continue');
    start(b);
    push({ screen: 'player', id: b.id });
  },
  'search-play'(d) {
    const b = bookById(d.id);
    if (state.playing && state.currentBookId === b.id) {
      state.playing = false;
      return render();
    }
    start(b);
    push({ screen: 'player', id: b.id });
  },
  'player-play'(d) {
    const b = bookById(d.id);
    anim.morph = true;
    if (state.playing && state.currentBookId === b.id) state.playing = false;
    else start(b);
    render();
  },
  'player-skip'(d) {
    const b = bookById(topEntry().id);
    b.position = Math.max(0, Math.min(b.duration, b.position + Number(d.value)));
    if (b.position >= b.duration) return finish(b);
    b.finished = false;
    changed();
    render();
  },
  'player-end': d => finish(bookById(d.id)),
  'up-next'(d) {
    const b = bookById(d.id);
    done('next-book');
    start(b);
    state.nav[state.nav.length - 1] = { screen: 'player', id: b.id };
    anim.screen = 'forward';
    render();
  },

  'sort-by'(d) {
    state.prefs.sort = d.value;
    render();
  },
  'toggle-finished'() {
    state.prefs.showFinished = !state.prefs.showFinished;
    render();
  },
  'toggle-hidden'() {
    state.prefs.showHidden = !state.prefs.showHidden;
    render();
  },
  'toggle-source'(d) {
    const { off } = state.prefs;
    if (off.has(d.value)) off.delete(d.value);
    else off.add(d.value);
    render();
  },
  layout(d) {
    state.prefs.layout = d.value;
    render();
  },
  theme(d) {
    state.prefs.theme = d.value;
    render();
  },
  reset() {
    state = freshState({ layout: state.prefs.layout, theme: state.prefs.theme });
    index = null;
    anim.entrance = true;
    render();
  },
  'add-folder'() {
    state.preview = { choices: Object.fromEntries(PREVIEW.items.map(i => [i.key, i.guess])) };
    closeSheet(() => push({ screen: 'preview' }));
  },
  'preview-item': d => openSheet({ type: 'organize', key: d.key }),
  organize(d) {
    state.preview.choices[d.key] = d.value;
    render();
    setTimeout(() => closeSheet(), 220);
  },
  'preview-add': addPreview,

  status(d) {
    setStatus(bookById(d.id), d.value);
    render();
  },
  edit(d) {
    const b = bookById(d.id);
    openSheet({ type: 'edit', id: b.id, active: null, draft: { title: titleOf(b), author: authorOf(b) ?? '', series: seriesOf(b) ?? '', part: partOf(b) ?? '' } });
  },
  cover: () => snack('Covers work like today, so they aren’t part of the prototype.'),
  delete: () => snack('Deleting works like today, so it isn’t part of the prototype.'),
  split(d) {
    const b = bookById(d.id);
    closeSheet(() => split(b));
  },
  hide(d) {
    const b = bookById(d.id);
    closeSheet(() => hide([b], `${titleOf(b)} is hidden`));
  },
  unhide(d) {
    const b = bookById(d.id);
    b.hidden = false;
    changed();
    done('hide-undo');
    closeSheet(() => snack(`${titleOf(b)} is back in the library`));
  },
  select(d) {
    const b = bookById(d.id);
    closeSheet(() => startSelecting([b]));
  },

  rename(d) {
    state.menu = null;
    openSheet({ type: 'rename', kind: d.kind, key: d.key, draft: stackName(d.kind, d.key) });
  },
  'hide-stack'(d) {
    const books = stackBooks(d.kind, d.key);
    const text = d.kind === 'series' ? `${stackName(d.kind, d.key)} is hidden` : `${plural(books.length, 'book')} by ${stackName(d.kind, d.key)} are hidden`;
    state.menu = null;
    if (state.sheet) closeSheet(() => hide(books, text));
    else hide(books, text);
  },
  'unhide-stack'(d) {
    stackBooks(d.kind, d.key).forEach(b => {
      b.hidden = false;
    });
    changed();
    done('hide-undo');
    state.menu = null;
    if (state.sheet) closeSheet();
    else render();
  },
  'select-stack'(d) {
    const books = stackBooks(d.kind, d.key);
    state.menu = null;
    if (state.sheet) closeSheet(() => startSelecting(books));
    else startSelecting(books);
  },
  join(d) {
    state.menu = null;
    if (state.sheet) closeSheet(() => join(d.key));
    else join(d.key);
  },

  pick(d) {
    const s = state.sheet;
    if (s.type === 'edit') s.draft[d.field] = d.value;
    else s.draft = d.value;
    if (s.type === 'edit') s.active = null;
    render();
  },
  'save-edit'() {
    const s = state.sheet;
    const b = bookById(s.id);
    const clean = v => String(v ?? '').trim();
    const part = clean(s.draft.part) === '' ? null : Number(clean(s.draft.part));
    const next = {
      title: clean(s.draft.title) || b.title,
      author: clean(s.draft.author) || null,
      series: clean(s.draft.series) || null,
      part: Number.isFinite(part) ? part : null,
    };
    if (!next.series) next.part = null;
    for (const key of Object.keys(next)) {
      if (next[key] === (b[key] ?? null)) delete b.edit[key];
      else b.edit[key] = next[key];
    }
    changed();
    closeSheet();
  },
  'reset-edit'() {
    const b = bookById(state.sheet.id);
    b.edit = {};
    changed();
    closeSheet(() => snack('Back to the names from the files'));
  },
  'save-rename'() {
    const { kind, key, draft } = state.sheet;
    const name = draft.trim();
    if (!name) return;
    const map = kind === 'series' ? getIndex().series : getIndex().authors;
    const newKey = norm(name);
    const joined = newKey !== key && map.has(newKey);
    const books = map.get(key).books.filter(b => kind === 'series' || !isServer(b));
    const snap = snapshot(books);
    const nav = state.nav.map(e => ({ ...e }));
    books.forEach(b => {
      b.edit[kind] = name;
    });
    changed();
    state.nav.forEach(e => {
      if (e.screen === kind && e.key === key) e.key = newKey;
    });
    if (kind === 'author' && joined && key === 'hartwelldaniel') done('author-join');
    closeSheet(() => snack(joined ? `Joined with ${map.get(newKey)?.name ?? name}` : `Renamed to ${name}`, () => {
      restore(snap);
      state.nav = nav;
    }));
  },

  'sel-close'() {
    state.selection = null;
    render();
  },
  'sel-all'() {
    shownBooks().forEach(b => state.selection.ids.add(b.id));
    render();
  },
  'sel-series': () => openSheet({ type: 'bulk', field: 'series', ids: [...state.selection.ids], draft: '' }),
  'sel-author': () => openSheet({ type: 'bulk', field: 'author', ids: [...state.selection.ids], draft: '' }),
  'sel-hide'() {
    const books = [...state.selection.ids].map(bookById);
    state.selection = null;
    hide(books, books.length === 1 ? `${titleOf(books[0])} is hidden` : `${plural(books.length, 'book')} are hidden`);
  },
  'save-bulk'() {
    const { field: name, ids: list, draft } = state.sheet;
    const text = draft.trim();
    if (!text) return;
    const books = list.map(bookById).filter(Boolean);
    const target = books.filter(b => !isServer(b));
    const left = books.length - target.length;
    const snap = snapshot(target);
    target.forEach(b => {
      b.edit[name] = text;
      // books put in a series follow their folder or file names
      if (name === 'series') b.edit.part = null;
    });
    changed();
    if (name === 'series' && target.length >= 2 && target.every(b => b.id.startsWith('lantern-'))) done('bulk-series');
    state.selection = null;
    const message = name === 'series' ? `Put ${plural(target.length, 'book')} in ${text}` : `Set the author of ${plural(target.length, 'book')}`;
    closeSheet(() => snack(`${message}${left ? `, left out ${left} from Audiobookshelf` : ''}`, () => restore(snap)));
  },

  'snack-undo'() {
    const undo = state.snackbar?.undo;
    state.snackbar = null;
    if (undo) undo();
    render();
  },
  'search-clear'() {
    state.search.query = '';
    state.search.filter = 'all';
    anim.focus = 'q';
    render();
  },
  'search-filter'(d) {
    state.search.filter = d.value;
    render();
  },
  'browse-tab'(d) {
    state.search.browse = d.value;
    render();
  },
  'browse-name'(d) {
    state.search.query = d.value;
    state.search.filter = d.field;
    render();
  },
};

let press = null;
let lastLongPress = 0;
let swallowClick = false;

function longPress(el) {
  lastLongPress = Date.now();
  swallowClick = true;
  setTimeout(() => {
    swallowClick = false;
  }, 450);
  navigator.vibrate?.(8);
  const d = el.dataset;
  if (selecting()) return;
  if (d.long === 'book') openSheet({ type: 'book', id: d.id });
  else openSheet({ type: 'stack', kind: d.long, key: d.key });
}

function cancelPress() {
  if (!press) return;
  clearTimeout(press.timer);
  press.el.classList.remove('pressing');
  press = null;
}

root.addEventListener('pointerdown', e => {
  const el = e.target.closest('[data-long]');
  if (!el || e.button !== 0 || e.target.closest('.play-btn')) return;
  cancelPress();
  el.classList.add('pressing');
  press = {
    el,
    x: e.clientX,
    y: e.clientY,
    timer: setTimeout(() => {
      const target = press.el;
      cancelPress();
      longPress(target);
    }, 480),
  };
});
root.addEventListener('pointermove', e => {
  if (press && Math.hypot(e.clientX - press.x, e.clientY - press.y) > 8) cancelPress();
});
['pointerup', 'pointercancel', 'pointerleave'].forEach(type => root.addEventListener(type, cancelPress));
root.addEventListener('scroll', cancelPress, true);

root.addEventListener('contextmenu', e => {
  const el = e.target.closest('[data-long]');
  if (!el) return;
  e.preventDefault();
  cancelPress();
  if (Date.now() - lastLongPress > 700) longPress(el);
});

function dispatch(e) {
  const el = e.target.closest('[data-action]');
  if (!el || el.disabled) return;
  if (swallowClick && root.contains(el)) {
    swallowClick = false;
    return;
  }
  if (closing) return;
  const fn = ACTIONS[el.dataset.action];
  if (!fn) return;
  e.preventDefault();
  e.stopPropagation();
  fn(el.dataset, el, e);
}
root.addEventListener('click', dispatch);
notesEl.addEventListener('click', dispatch);

root.addEventListener('keydown', e => {
  const el = e.target.closest('[data-action][role="button"]');
  if (el && el === e.target && (e.key === 'Enter' || e.key === ' ')) {
    e.preventDefault();
    el.click();
  }
  if (e.key === 'Enter' && e.target.id === 'q') e.target.blur();
});

document.addEventListener('keydown', e => {
  if (e.key !== 'Escape') return;
  if (state.menu) {
    state.menu = null;
    render();
  } else if (state.sheet) closeSheet();
  else if (selecting()) ACTIONS['sel-close']();
  else pop();
});

function updateSearch() {
  document.getElementById('search-body').innerHTML = searchBody();
  document.getElementById('q-clear')?.classList.toggle('gone', !state.search.query);
  const model = searchModel(state.search.query);
  const numbers = model?.tokens.filter(isNumber).map(Number) ?? [];
  if (numbers.includes(63) && model.books[0]?.book.id === 'nachtfalken-63') {
    done('number');
    renderNotes();
  }
}

function updateSuggestions() {
  root.querySelectorAll('.suggest').forEach(el => {
    el.innerHTML = suggestions(el.id.slice(2));
  });
}

root.addEventListener('input', e => {
  const t = e.target;
  if (t.id === 'q') {
    state.search.query = t.value;
    state.search.filter = 'all';
    updateSearch();
  } else if (t.dataset.seek) {
    const b = bookById(t.dataset.seek);
    b.position = Number(t.value);
    if (b.position < b.duration) b.finished = false;
    t.style.setProperty('--p', `${((b.position / b.duration) * 100).toFixed(2)}%`);
    document.getElementById('times').innerHTML = timesHtml(b);
  } else if (t.dataset.field && state.sheet) {
    const s = state.sheet;
    if (s.type === 'edit') {
      s.draft[t.dataset.field] = t.value;
      s.active = t.dataset.field;
    } else {
      s.draft = t.value;
    }
    updateSuggestions();
  }
});

root.addEventListener('change', e => {
  const t = e.target;
  if (!t.dataset.seek) return;
  const b = bookById(t.dataset.seek);
  if (b.position >= b.duration) return finish(b);
  b.lastListenedAt = Date.now();
  changed();
  render();
});

root.addEventListener('focusin', e => {
  const name = e.target.dataset?.field;
  if (name && state.sheet?.type === 'edit' && state.sheet.active !== name) {
    state.sheet.active = name;
    updateSuggestions();
  }
});

darkQuery.addEventListener('change', render);
window.addEventListener('resize', () => root.style.setProperty('--cols', Math.max(2, Math.round((root.clientWidth - 32) / 137))));

const NOTES = {
  'library-progress': ['Sorted by progress', 'The order of today, and the default. Two or more books of a series are one stack. A book in progress stands for its series: it shows on its own, with its series and part. A series with no book in progress is in Not started yet before its first book, in Current between two books, and in Completed after its last.', [['1. Series in the library', '1-series-in-the-library'], ['5. Sorting and filtering', '5-sorting-and-filtering']]],
  'library-title': ['Sorted by title', 'Current stays on top, in the order you listened. Below it, All books has the whole library in one list: every book once, also the ones in progress and the finished ones. Two or more books of a series are one stack.', [['5. Sorting and filtering', '5-sorting-and-filtering']]],
  'library-author': ['Sorted by author', 'Two or more series or books of an author are one stack with all their books, and a tap opens the author. An author with one series or book shows it as it is. Books with no author come last. Authors sort by their name as written, so “Hartwell, Daniel” comes under H.', [['Sorted by author', 'sorted-by-author']]],
  'library-recent': ['Sorted by recently added', 'Books from the last 30 days are under the day they were added, older ones under their month. Within a day or month they sort by title, so a folder added at once is easy to scan. A series goes where its newest book is. Books added before Voice kept the date are under Earlier.', [['Sorted by recently added', 'sorted-by-recently-added']]],
  sort: ['Sort and show', 'One place for the order and for what the library shows. Current is always on top. Without finished books, Completed is gone. Hidden books come back here. From only shows up with more than one folder or server. The dot on the button says a filter is on.', [['5. Sorting and filtering', '5-sorting-and-filtering']]],
  actions: ['Book actions', 'The author and the series open their screens. Edit replaces Rename. Hide from library takes the book out of the library, Continue listening, the widget and Android Auto, while progress, bookmarks and files stay, and search still finds it. Select starts selecting.', [['6. Hiding books', '6-hiding-books'], ['7. Editing', '7-editing-title-author-and-series']]],
  edit: ['Edit', 'Title, author, series and part. Author and series suggest the names in the library, so one name isn’t there twice. Edits never touch the files, and what wasn’t edited still follows them. “Use the names from the files” undoes all edits.', [['7. Editing', '7-editing-title-author-and-series']]],
  stack: ['A series or an author', 'A long press on a stack renames or hides it. Picking a suggested name joins two series or two authors. Renaming an author leaves out books from Audiobookshelf.', [['2. The series', '2-the-series'], ['3. The author', '3-the-author']]],
  select: ['Selecting books', 'A tap adds a book, a tap on a stack all its books, and Select all everything shown. Series, Author and Hide change all of them. Books put in a series follow their file names. Series and Author leave out books from Audiobookshelf.', [['Several books at once', 'several-books-at-once']]],
  series: ['The series', 'Books in the order of their parts. Books without a part follow their file names, with 2 before 10. One button continues the book in progress or starts the first one that isn’t finished.', [['2. The series', '2-the-series']]],
  author: ['The author', 'Their series first, as stacks, then their other books. Names that only differ in case, spaces or punctuation are one author: Interference is tagged “daniel hartwell”. Other spellings, like “Hartwell, Daniel”, join with a rename.', [['3. The author', '3-the-author']]],
  search: ['Search', 'The best match stays on top and plays in one tap. Authors and series that match come right below it. Books in a series and hidden books are still found one by one. Numbers match as a whole, so 63 finds Folge 063 but not Folge 163. Series and part work together: try “signal saga 3”. Narrators and genres match whole names.', [['9. Search', '9-search']]],
  player: ['The next book', 'When a book of a series ends, the player offers the next one: the first unfinished book after it. It never starts on its own, so a book that ends at night stays quiet. Continue listening in the library offers it too, until another book is started.', [['8. The next book in a series', '8-the-next-book-in-a-series']]],
  preview: ['Voice finds the books', 'Instead of three layouts to pick from, Voice looks at each folder on its own. A folder that looks like one book is one book, any other folder holds books. A tap on one fixes what it got wrong. This would start as an A/B test with new listeners.', [['10. Voice finds the books', '10-voice-finds-the-books']]],
  settings: ['Prototype settings', 'Switch the layout and the theme, add a folder to see what “Voice finds the books” makes of it, or start over.', []],
};

const TASKS = [
  ['series-continue', 'Continue The Signal Saga from its series screen.', 'Tap the series in the actions of Echoes of Tomorrow, or search for it.'],
  ['next-book', 'Finish Echoes of Tomorrow, then start the next book.', 'Open it, and skip to the end in the player.'],
  ['author-stack', 'Sort by author, then open Lena Morgan.', 'The sort button is next to search.'],
  ['number', 'Find Folge 63 of Die Nachtfalken by its number.', 'Search for 63.'],
  ['bulk-series', 'Put The Lantern Keepers 1 to 6 in one series.', 'Search for lantern, long press a book, Select, Select all, then Series.'],
  ['author-join', 'Join “Hartwell, Daniel” with Daniel Hartwell.', 'Open the author of Night Shift Radio, then rename.'],
  ['hide-undo', 'Hide a book, then bring it back.', 'Undo, or show hidden books in the sort sheet.'],
  ['split', 'Split Die Sternenpiraten into its episodes.', 'It’s one book of 12 files in the Kids folder.'],
  ['folder', 'Add a folder, and fix what Voice found.', 'Settings, then Add a folder.'],
];

function noteKey() {
  const sheet = state.sheet?.type;
  if (sheet === 'bulk' || selecting()) return 'select';
  if (sheet) return { sort: 'sort', settings: 'settings', book: 'actions', stack: 'stack', rename: 'stack', edit: 'edit', organize: 'preview' }[sheet];
  const screen = topEntry().screen;
  return screen === 'library' ? `library-${state.prefs.sort}` : screen;
}

function renderNotes() {
  const [title, text, links] = NOTES[noteKey()];
  const p = state.prefs;
  const doneCount = state.tasks.size;
  const popped = anim.task;
  anim.task = null;
  const tasks = TASKS.map(([id, task, hint]) => `<li class="task ${state.tasks.has(id) ? 'done' : ''} ${popped === id ? 'pop' : ''}"><span class="tick">${icon('check')}</span><span><span class="task-title">${task}</span><span class="task-hint">${hint}</span></span></li>`).join('');
  notesEl.innerHTML = `
    <header class="notes-head"><p class="eyebrow">Voice</p><h1>Library prototype</h1>
      <p>A clickable draft of the <a href="${SPEC}">library spec</a>. Nothing here is built yet. The books are made up, and what you change stays in this tab.</p>
      <p class="muted">Tap to open. Long press or right-click for the actions.</p></header>
    <div class="controls">${segmented('layout', [['list', 'List', 'list'], ['grid', 'Grid', 'grid']], p.layout)}${segmented('theme', [['system', 'System'], ['light', 'Light'], ['dark', 'Dark']], p.theme)}</div>
    <section class="card now" aria-live="polite"><p class="eyebrow">On screen</p><h2>${title}</h2><p>${text}</p>${links.length ? `<p class="spec-links">${links.map(([label, anchor]) => `<a href="${SPEC}#${anchor}">${label}</a>`).join('')}</p>` : ''}</section>
    <section class="card"><h2>Try it <span class="progress-count">${doneCount} of ${TASKS.length}</span></h2><ol class="tasks">${tasks}</ol>
      <button class="text-btn" data-action="reset">${icon('restart')}Start over</button></section>
    <section class="card quiet"><h2>Not in the prototype</h2><p>Android Auto and the widgets, syncing through <code>.voice</code>, series folders with “Folders are authors”, and the A/B test itself.</p></section>`;
}

state = freshState({});
render();
