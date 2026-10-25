const WHITE = { fill: '#fff', fillStyle: 'solid' };

function topBar(x, y) {
  roundRect(x + 16, y + 40, 232, 48, 24);
  icon.search(x + 42, y + 64);
  text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
  const sort = { x: x + 278, y: y + 64 };
  icon.sort(sort.x, sort.y);
  circle(x + 324, y + 64, 22);
  circle(x + 324, y + 64, 8);
  return sort;
}

function header(x, y, title, count) {
  const t = text(x, y, title, { size: 20 });
  const w = String(count).length * 7 + 18;
  roundRect(t.x + t.width + 10, y - 17, w, 22, 11, { stroke: GREY });
  text(t.x + t.width + 10 + w / 2, y - 1, String(count), { size: 12, weight: 700, anchor: 'middle' });
}

function stack(cx, cy, s, r) {
  roundRect(cx + s * 0.08, cy, s * 0.92, s * 0.92, r, { stroke: LIGHT, ...WHITE });
  roundRect(cx + s * 0.04, cy + s * 0.04, s * 0.92, s * 0.92, r, { stroke: GREY, ...WHITE });
  roundRect(cx, cy + s * 0.08, s * 0.92, s * 0.92, r, WHITE);
  image(cx, cy + s * 0.08, s * 0.92, s * 0.92, r);
}

function mosaic(cx, cy, s, r) {
  const gap = s * 0.05;
  const c = (s - gap) / 2;
  for (const [dx, dy] of [[0, 0], [1, 0], [0, 1], [1, 1]]) image(cx + dx * (c + gap), cy + dy * (c + gap), c, c, r / 2);
}

function badge(bx, by, count) {
  roundRect(bx, by, 30, 22, 11, { ...WHITE, strokeWidth: 1.4 });
  text(bx + 15, by + 16, String(count), { size: 12, weight: 700, anchor: 'middle' });
}

function listRow(x, y, { title, author, progress, caption, kind }) {
  roundRect(x + 16, y, 328, 84, 18);
  if (kind === 'series') stack(x + 26, y + 10, 64, 12);
  else if (kind === 'author') mosaic(x + 26, y + 10, 64, 12);
  else image(x + 26, y + 10, 64, 64, 12);
  text(x + 106, y + (author ? 30 : 38), title, { size: 15 });
  if (author) text(x + 106, y + 48, author, { size: 12, fill: GREY });
  if (progress) {
    line(x + 106, y + 66, x + 106 + 118 * progress, y + 66, { strokeWidth: 4 });
    line(x + 106 + 118 * progress + 4, y + 66, x + 224, y + 66, { stroke: LIGHT, strokeWidth: 4 });
    text(x + 332, y + 70, caption, { size: 11, fill: GREY, anchor: 'end' });
  } else {
    text(x + 106, y + (author ? 70 : 58), caption, { size: 12, fill: GREY });
  }
  return y;
}

function gridTile(cx, top, { title, subtitle, count, kind, progress }) {
  if (kind === 'series') stack(cx, top, 148, 18);
  else if (kind === 'author') mosaic(cx, top, 148, 18);
  else image(cx, top, 148, 148, 18);
  if (count) badge(cx + 108, top + 118, count);
  let ty = top + 170;
  if (progress) {
    line(cx, top + 160, cx + 148 * progress, top + 160, { strokeWidth: 4 });
    line(cx + 148 * progress + 4, top + 160, cx + 148, top + 160, { stroke: LIGHT, strokeWidth: 4 });
    ty += 14;
  }
  text(cx, ty, title, { size: 14, weight: 700 });
  text(cx, ty + 18, subtitle, { size: 12, fill: GREY });
  return { x: cx, top };
}

const p = phone(300, 80, 'List, sorted by author');
const { x, y } = p;
topBar(x, y);

header(x + 20, y + 130, 'Current', 2);
listRow(x, y + 144, { title: 'Echoes of Tomorrow', author: 'Daniel Hartwell', progress: 0.72, caption: '4:03:15 left' });
listRow(x, y + 230, { title: 'Beyond the Horizon', author: 'Alexander Reeves', progress: 0.3, caption: '7:26:05 left' });

header(x + 20, y + 356, 'All books', '1,204');
const lena = listRow(x, y + 370, { title: 'Lena Morgan', caption: '2 series · 9 books · 3 finished', kind: 'author' });
const lantern = listRow(x, y + 456, { title: 'The Lantern Keepers', author: 'Mara Quill', progress: 0.2, caption: '1 of 6 finished', kind: 'series' });
const salt = listRow(x, y + 542, { title: 'Salt and Silence', author: 'Nora Whitfield', caption: 'Completed' });
listRow(x, y + 628, { title: 'Ruth Calloway', caption: '14 books', kind: 'author' });

note(20, y + 150, ['Current stays on', 'top, in the order', 'you listened.']);
arrow([[200, y + 230], [250, y + 236], [x + 10, y + 228]]);

note(20, y + 370, ['Below it, the whole', 'library. Every author', 'once, with all their', 'books.']);
arrow([[200, y + 470], [250, y + 450], [x + 22, lena + 42]]);

note(20, y + 590, ['Just one series', 'or book stays', 'as it is.']);
arrow([[180, y + 560], [250, y + 520], [x + 22, lantern + 50]]);
arrow([[200, y + 640], [250, y + 616], [x + 22, salt + 44]]);

const q = phone(780, 80, 'Grid, sorted by author');
const gx = q.x;
const sort = topBar(gx, y);

header(gx + 20, y + 136, 'All books', '1,204');
gridTile(gx + 22, y + 152, { title: 'Lena Morgan', subtitle: '2 series · 9 books', count: 9, kind: 'author' });
gridTile(gx + 190, y + 152, { title: 'The Lantern Keepers', subtitle: 'Mara Quill', count: 6, kind: 'series', progress: 0.2 });
gridTile(gx + 22, y + 384, { title: 'Salt and Silence', subtitle: 'Nora Whitfield' });
const ruth = gridTile(gx + 190, y + 384, { title: 'Ruth Calloway', subtitle: '14 books', count: 14, kind: 'author' });

note(gx + 400, y + 20, ['Only when you', 'sort by author.']);
arrow([[gx + 396, y + 40], [gx + 340, y + 20], [sort.x + 6, sort.y - 16]]);

note(gx + 400, y + 430, ['A tap opens the', 'author, with all', 'their series and', 'books.']);
arrow([[gx + 396, y + 460], [gx + 372, y + 450], [ruth.x + 152, ruth.top + 60]]);
