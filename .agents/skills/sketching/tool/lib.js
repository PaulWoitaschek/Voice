const INK = '#2b2b2b';
const GREY = '#8c8c8c';
const LIGHT = '#c9c9c9';
const NOTE = '#e8590c';
const HATCH = { fill: '#c4c4c4', fillStyle: 'hachure', hachureGap: 5 };
const SOLID = { fill: INK, fillStyle: 'solid' };
const PIN = { fill: '#9a9a9a', fillStyle: 'hachure', hachureGap: 2.6, strokeWidth: 1.3 };

const svg = document.getElementById('sketch');
const rc = rough.svg(svg, { options: { seed: 11, roughness: 1.1, bowing: 0.8, stroke: INK, strokeWidth: 1.6 } });

window.fontsLoaded = Promise.all(['400 16px UI', '700 16px UI', '600 16px Hand'].map((font) => document.fonts.load(font)));

function fit(margin = 24) {
  const box = svg.getBBox();
  const width = Math.ceil(box.width + margin * 2);
  const height = Math.ceil(box.height + margin * 2);
  svg.setAttribute('viewBox', `${box.x - margin} ${box.y - margin} ${width} ${height}`);
  svg.setAttribute('width', width);
  svg.setAttribute('height', height);
}

const add = (node) => svg.appendChild(node);

// Shapes. `style` takes rough.js options, like { stroke: GREY }, HATCH or SOLID.
const rect = (x, y, w, h, style = {}) => add(rc.rectangle(x, y, w, h, style));
const line = (x1, y1, x2, y2, style = {}) => add(rc.line(x1, y1, x2, y2, style));
const circle = (cx, cy, d, style = {}) => add(rc.circle(cx, cy, d, style));
const path = (d, style = {}) => add(rc.path(d, style));
const poly = (points, style = {}) => add(rc.polygon(points, style));
const curve = (points, style = {}) => add(rc.curve(points, style));

function roundRect(x, y, w, h, r, style = {}) {
  r = Math.min(r, h / 2, w / 2);
  return path(
    `M${x + r},${y} H${x + w - r} A${r},${r} 0 0 1 ${x + w},${y + r} V${y + h - r} A${r},${r} 0 0 1 ${x + w - r},${y + h} ` +
      `H${x + r} A${r},${r} 0 0 1 ${x},${y + h - r} V${y + r} A${r},${r} 0 0 1 ${x + r},${y} Z`,
    style,
  );
}

// y is the baseline. Returns the box of the text: { x, y, width, height }.
function text(x, y, str, { size = 14, weight = 400, fill = INK, anchor = 'start', font = 'UI' } = {}) {
  const t = document.createElementNS('http://www.w3.org/2000/svg', 'text');
  Object.entries({
    x,
    y,
    'font-family': font,
    'font-size': size,
    'font-weight': weight,
    fill,
    'text-anchor': anchor,
  }).forEach(([k, v]) => t.setAttribute(k, v));
  t.textContent = str;
  add(t);
  return t.getBBox();
}

// Parts of one line, one after another, each with its own style: run(x, y, ['Ch. 3', { str: '→', pad: 6, style: { fill: GREY } }, '12:40']).
// Returns the box of each part, to point arrows at.
function run(x, y, parts, style = {}) {
  return parts.map((part) => {
    const p = typeof part === 'string' ? { str: part } : part;
    const box = text(x + (p.pad || 0), y, p.str, { ...style, ...p.style });
    x = box.x + box.width + (p.pad || 0);
    return box;
  });
}

// Annotations: orange handwriting, and an arrow from the note to what it explains.
function note(x, y, lines, { size = 25, anchor = 'start' } = {}) {
  lines.forEach((l, i) => text(x, y + i * size * 1.05, l, { size, font: 'Hand', fill: NOTE, anchor, weight: 600 }));
}

function arrow(points) {
  const style = { stroke: NOTE, strokeWidth: 2.2, roughness: 1.4 };
  if (points.length === 2) line(...points[0], ...points[1], style);
  else curve(points, style);
  const [x2, y2] = points[points.length - 1];
  const [x1, y1] = points[points.length - 2];
  const a = Math.atan2(y2 - y1, x2 - x1);
  for (const s of [-1, 1]) line(x2, y2, x2 - 13 * Math.cos(a + s * 0.45), y2 - 13 * Math.sin(a + s * 0.45), style);
}

// Icons, centered on (x, y). s is about half the icon's size.
const icon = {
  chevronDown(x, y, s = 8) { line(x - s, y - s / 2, x, y + s / 2); line(x, y + s / 2, x + s, y - s / 2); },
  chevronRight(x, y, s = 8) { line(x - s / 2, y - s, x + s / 2, y); line(x + s / 2, y, x - s / 2, y + s); },
  back(x, y, s = 9) { line(x - s, y, x + s, y); line(x - s, y, x - s + 7, y - 7); line(x - s, y, x - s + 7, y + 7); },
  close(x, y, s = 8) { line(x - s, y - s, x + s, y + s); line(x + s, y - s, x - s, y + s); },
  check(x, y, s = 8) { line(x - s, y, x - s / 3, y + s * 0.7); line(x - s / 3, y + s * 0.7, x + s, y - s * 0.7); },
  plus(x, y, s = 9) { line(x - s, y, x + s, y); line(x, y - s, x, y + s); },
  search(x, y, s = 9) { circle(x - 2, y - 2, s * 1.4); line(x + s * 0.4, y + s * 0.4, x + s, y + s); },
  chapters(x, y) { roundRect(x - 11, y - 9, 22, 18, 3); line(x - 11, y - 2, x + 11, y - 2); line(x - 11, y + 4, x + 11, y + 4); line(x - 4, y - 9, x - 4, y + 9); },
  more(x, y) { for (const d of [-7, 0, 7]) circle(x, y + d, 3, SOLID); },
  sort(x, y) { line(x - 10, y - 7, x + 10, y - 7); line(x - 10, y, x + 5, y); line(x - 10, y + 7, x, y + 7); },
  play(x, y, s = 14) { poly([[x - s * 0.6, y - s], [x - s * 0.6, y + s], [x + s, y]], SOLID); },
  pause(x, y, s = 13) { rect(x - s * 0.6, y - s, s * 0.4, s * 2, SOLID); rect(x + s * 0.2, y - s, s * 0.4, s * 2, SOLID); },
  previous(x, y, s = 9) { line(x - s, y - s, x - s, y + s); poly([[x + s, y - s], [x + s, y + s], [x - s / 2, y]]); },
  next(x, y, s = 9) { line(x + s, y - s, x + s, y + s); poly([[x - s, y - s], [x - s, y + s], [x + s / 2, y]]); },
  rewind(x, y, s = 13) { turn(x, y, s, false); },
  forward(x, y, s = 13) { turn(x, y, s, true); },
  moon(x, y, s = 10, style = {}) { path(`M${x + s * 0.3},${y - s} A${s},${s} 0 1 0 ${x + s},${y + s * 0.35} A${s * 0.8},${s * 0.8} 0 0 1 ${x + s * 0.3},${y - s} Z`, style); },
  speed(x, y) { path(`M${x - 11},${y + 6} A12,12 0 1 1 ${x + 11},${y + 6}`); line(x, y + 3, x + 6, y - 5); },
  bookmark(x, y, s = 10, style = {}) { path(`M${x - s * 0.7},${y - s} H${x + s * 0.7} V${y + s} L${x},${y + s * 0.45} L${x - s * 0.7},${y + s} Z`, style); },
  skipSilence(x, y) { circle(x - 6, y + 7, 8); circle(x + 6, y + 7, 8); line(x - 4, y + 3, x + 8, y - 11); line(x + 4, y + 3, x - 8, y - 11); },
  volume(x, y) { poly([[x - 11, y - 4], [x - 6, y - 4], [x, y - 10], [x, y + 10], [x - 6, y + 4], [x - 11, y + 4]]); path(`M${x + 4},${y - 6} A8,8 0 0 1 ${x + 4},${y + 6}`); },
  // the bookmark kinds, in the shapes of bookmarkStyle
  favorite(x, y, s = 8, style = PIN) { path(`M${x},${y + s} C${x - s * 1.6},${y - s * 0.1} ${x - s * 0.9},${y - s * 1.3} ${x},${y - s * 0.4} C${x + s * 0.9},${y - s * 1.3} ${x + s * 1.6},${y - s * 0.1} ${x},${y + s} Z`, style); },
  quote(x, y, s = 8, style = PIN) { poly([0, 1, 2, 3, 4, 5].map((i) => [x + s * Math.cos(Math.PI / 6 + i * Math.PI / 3), y + s * Math.sin(Math.PI / 6 + i * Math.PI / 3)]), style); },
  revisit(x, y, s = 8, style = PIN) { poly([...Array(16).keys()].map((i) => [x + (i % 2 ? s * 0.62 : s) * Math.cos(i * Math.PI / 8), y + (i % 2 ? s * 0.62 : s) * Math.sin(i * Math.PI / 8)]), style); },
  note(x, y, s = 8, style = PIN) { for (const [dx, dy] of [[-1, -1], [1, -1], [-1, 1], [1, 1]]) circle(x + dx * s * 0.44, y + dy * s * 0.44, s * 1.1, style); },
};

function turn(x, y, r, clockwise) {
  const f = clockwise ? -1 : 1;
  const end = (clockwise ? -30 : -150) * Math.PI / 180;
  path(`M${x},${y - r} A${r},${r} 0 1 ${clockwise ? 0 : 1} ${x + r * Math.cos(end)},${y + r * Math.sin(end)}`);
  line(x, y - r, x + f * 6, y - r - 6);
  line(x, y - r, x + f * 6, y - r + 6);
}

// Components. Sizes follow the app roughly; a phone is 360 × 760.
function phone(x, y, title) {
  roundRect(x, y, 360, 760, 38, { strokeWidth: 2.6 });
  if (title) text(x + 180, y - 22, title, { size: 30, font: 'Hand', anchor: 'middle', weight: 600 });
  return { x, y, w: 360, h: 760 };
}

function image(x, y, w, h, r = 16) {
  roundRect(x, y, w, h, r);
  const inset = Math.min(w, h) * 0.06;
  line(x + inset, y + inset, x + w - inset, y + h - inset, { stroke: LIGHT });
  line(x + w - inset, y + inset, x + inset, y + h - inset, { stroke: LIGHT });
}

function iconButton(x, y, name, { d = 44, style = {} } = {}) {
  circle(x, y, d, style);
  icon[name](x, y);
}

function button(x, y, w, h, label, { filled = false, icon: name } = {}) {
  roundRect(x, y, w, h, h / 2, filled ? HATCH : {});
  const textX = name ? x + 46 : x + w / 2;
  if (name) icon[name](x + 26, y + h / 2);
  return text(textX, y + h / 2 + 5, label, { size: 14, weight: 700, anchor: name ? 'start' : 'middle' });
}

function chip(x, y, w, label, { selected = false, kind } = {}) {
  roundRect(x, y, w, 30, 15, selected ? HATCH : {});
  if (kind) icon[kind](x + 18, y + 15, 6);
  text(kind ? x + 30 : x + w / 2, y + 20, label, { size: 13, anchor: kind ? 'start' : 'middle' });
}

function tabs(x, y, w, labels, selected) {
  const tab = w / labels.length;
  labels.forEach((label, i) => {
    const on = i === selected;
    text(x + tab * i + tab / 2, y, label, { size: 15, anchor: 'middle', weight: on ? 700 : 400, fill: on ? INK : GREY });
  });
  line(x, y + 14, x + w, y + 14, { stroke: LIGHT, strokeWidth: 1.2 });
  const cx = x + tab * selected + tab / 2;
  line(cx - 45, y + 13, cx + 45, y + 13, { strokeWidth: 4 });
}

// The bar on top of a screen: back arrow, an optional cover, title and subtitle, and icons on the right.
function appBar(x, y, { title, subtitle, cover = false, actions = [] }) {
  icon.back(x + 34, y + 50);
  let left = x + 62;
  if (cover) {
    image(left, y + 32, 36, 36, 8);
    left += 48;
  }
  text(left, subtitle ? y + 47 : y + 56, title, { size: 16, weight: 700 });
  if (subtitle) text(left, y + 65, subtitle, { size: 12, fill: GREY });
  actions.forEach((name, i) => icon[name](x + 324 - i * 44, y + 50));
}

function label(x, y, str, { bold = false } = {}) {
  return text(x, y, str, { size: bold ? 13 : 12, weight: bold ? 700 : 400, fill: GREY });
}

// A card in a list: a leading icon or bookmark kind, a title, and up to two lines below.
function row(x, y, w, { icon: name, kind, title, subtitle, caption }) {
  const h = caption ? 76 : 58;
  roundRect(x, y, w, h, 16);
  if (kind) icon[kind](x + 28, y + h / 2, 11);
  else if (name) icon[name](x + 28, y + h / 2);
  const titles = Array.isArray(title) ? title : [title];
  const top = caption ? y + 26 : y + 25;
  const boxes = run(x + 56, top, titles, { size: 15 });
  if (subtitle) text(x + 56, top + 20, subtitle, { size: 12, fill: GREY });
  if (caption) text(x + 56, top + 38, caption, { size: 12, fill: GREY });
  return { x, y, w, h, title: boxes };
}

// The book as one segment per chapter. pins: [[spotInBook, kind]]. Returns where a spot in the book (0 to 1) is.
function bookBar(x, y, w, { progress = 0.5, current, segments = [4, 6, 5, 7, 5, 6, 4, 7, 6, 5, 6, 7, 5, 6, 4, 5, 6, 6], pins = [], marker = false } = {}) {
  const total = segments.reduce((a, b) => a + b, 0);
  const gap = 5;
  const avail = w - gap * (segments.length - 1);
  const spans = [];
  let left = x;
  let start = 0;
  segments.forEach((segment, i) => {
    const f = segment / total;
    const sw = avail * f;
    const isCurrent = current === undefined ? progress >= start && progress < start + f : i === current;
    spans.push({ left, sw, start, f });
    line(left + 1, y, left + sw - 1, y, {
      strokeWidth: isCurrent ? 7 : 4,
      stroke: progress >= start + f || isCurrent ? INK : LIGHT,
      roughness: 0.6,
    });
    left += sw + gap;
    start += f;
  });
  const at = (p) => {
    const span = spans.find((s) => p < s.start + s.f) || spans[spans.length - 1];
    return span.left + span.sw * ((p - span.start) / span.f);
  };
  for (const [p, kind] of pins) icon[kind](at(p), y - 18, 7);
  if (marker) circle(at(progress), y, 18, SOLID);
  return at;
}

function slider(x, y, w, progress) {
  const at = x + w * progress;
  line(x, y, at - 3, y, { strokeWidth: 4 });
  line(at + 3, y, x + w, y, { stroke: LIGHT, strokeWidth: 4 });
  line(at, y - 12, at, y + 12, { strokeWidth: 3 });
}

// A floating toolbar with icons, spread evenly. Returns the x of each icon.
function toolbar(x, y, w, icons) {
  roundRect(x, y, w, 52, 26, { ...HATCH, fill: '#d0d0d0', hachureGap: 6 });
  const step = w / icons.length;
  return icons.map((name, i) => {
    const cx = x + step * i + step / 2;
    icon[name](cx, y + 26);
    return cx;
  });
}
