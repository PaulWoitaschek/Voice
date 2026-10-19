const WHITE = { fill: '#fff', fillStyle: 'solid' };

function link(x1, y1, x2, y2) {
  const style = { stroke: GREY, strokeWidth: 1.4, roughness: 0.8 };
  line(x1, y1, x2, y2, style);
  const a = Math.atan2(y2 - y1, x2 - x1);
  for (const s of [-1, 1]) line(x2, y2, x2 - 10 * Math.cos(a + s * 0.45), y2 - 10 * Math.sin(a + s * 0.45), style);
}

function stack(cx, cy, s, r) {
  roundRect(cx + s * 0.08, cy, s * 0.92, s * 0.92, r, { stroke: LIGHT, ...WHITE });
  roundRect(cx + s * 0.04, cy + s * 0.04, s * 0.92, s * 0.92, r, { stroke: GREY, ...WHITE });
  roundRect(cx, cy + s * 0.08, s * 0.92, s * 0.92, r, WHITE);
  image(cx, cy + s * 0.08, s * 0.92, s * 0.92, r);
}

const TX = 40;
const TY = 130;
const ROW = 34;
const INDENT = 26;

const tree = [
  [0, 'Audiobooks/'],
  [1, 'Hobbit.m4b'],
  [1, 'Woolf/'],
  [2, 'Mrs Dalloway/'],
  [2, 'To the Lighthouse/'],
  [1, 'Rowling/'],
  [2, 'Harry Potter/'],
  [3, "1 Philosopher's Stone/"],
  [4, 'CD 1/'],
  [4, 'CD 2/'],
  [3, '2 Chamber of Secrets/'],
  [1, 'Die drei ???/'],
  [2, 'Folge 001.mp3 … Folge 160.mp3'],
  [1, 'The Swarm/'],
  [2, 'Part 1 - The Beginning/'],
  [2, 'Part 2 - The Escape/'],
];

text(TX, 70, 'In the folder', { size: 30, font: 'Hand', weight: 600 });

const rows = tree.map(([depth, name], i) => {
  const y = TY + i * ROW;
  const x = TX + depth * INDENT;
  const box = text(x, y, name, { size: 15, fill: name.endsWith('/') ? INK : GREY });
  return { depth, x, y, box };
});

rows.forEach((r, i) => {
  if (r.depth === 0) return;
  let p = i - 1;
  while (rows[p].depth !== r.depth - 1) p--;
  const parent = rows[p];
  line(parent.x + 8, r.y - 5, r.x - 6, r.y - 5, { stroke: LIGHT, strokeWidth: 1.3, roughness: 0.6 });
  const last = rows.slice(p + 1).findIndex((c) => c.depth < r.depth);
  const lastChild = (last === -1 ? rows.slice(p + 1) : rows.slice(p + 1, p + 1 + last)).filter((c) => c.depth === r.depth).pop();
  if (lastChild === r) line(parent.x + 8, parent.y + 6, parent.x + 8, r.y - 5, { stroke: LIGHT, strokeWidth: 1.3, roughness: 0.6 });
});

const RX = 560;
text(RX, 70, 'In the library', { size: 30, font: 'Hand', weight: 600 });

const results = [
  { title: 'Hobbit', sub: 'Author from the tags', from: 1 },
  { title: 'Mrs Dalloway', sub: 'Woolf', from: 3 },
  { title: 'To the Lighthouse', sub: 'Woolf', from: 4 },
  { title: 'Harry Potter', sub: 'Rowling · 2 books', from: 6, stacked: true },
  { title: 'Die drei ???', sub: '160 chapters', from: 11 },
  { title: 'The Swarm', sub: '2 books', from: 13, stacked: true },
];

const cards = results.map((r, i) => {
  const cy = TY - 26 + i * 92;
  roundRect(RX, cy, 300, 68, 18);
  if (r.stacked) stack(RX + 10, cy + 6, 54, 10);
  else image(RX + 10, cy + 10, 48, 48, 10);
  text(RX + 78, cy + 30, r.title, { size: 15 });
  text(RX + 78, cy + 50, r.sub, { size: 12, fill: GREY });
  const src = rows[r.from];
  link(src.box.x + src.box.width + 12, src.y - 5, RX - 8, cy + 34);
  return { y: cy };
});

note(TX - 30, TY + 8 * ROW - 6, ['Discs stay', 'chapters.'], { anchor: 'end' });
arrow([[TX - 26, TY + 8 * ROW + 6], [TX + 40, TY + 8 * ROW + 24], [rows[8].x - 10, rows[8].y - 2]]);

note(RX + 340, cards[3].y + 10, ['A number in the names', 'makes a series. The folder', 'around it is the author.']);
arrow([[RX + 336, cards[3].y + 22], [RX + 318, cards[3].y + 26], [RX + 304, cards[3].y + 34]]);

note(RX + 340, cards[4].y + 30, ['When in doubt, one book.', 'A tap splits it into', 'a series of 160.']);
arrow([[RX + 336, cards[4].y + 40], [RX + 318, cards[4].y + 38], [RX + 304, cards[4].y + 34]]);

note(RX + 340, cards[5].y + 50, ['"Part 1 - The Beginning"', "isn't a disc, so it's a", 'series. A tap joins it.']);
arrow([[RX + 336, cards[5].y + 58], [RX + 318, cards[5].y + 46], [RX + 304, cards[5].y + 34]]);
