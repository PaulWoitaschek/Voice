const WHITE = { fill: '#fff', fillStyle: 'solid' };

function topBar(x, y) {
  roundRect(x + 16, y + 40, 250, 48, 24);
  icon.search(x + 42, y + 64);
  text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
  circle(x + 290, y + 64, 40);
  roundRect(x + 281, y + 57, 18, 14, 3);
  circle(x + 330, y + 64, 40);
  circle(x + 330, y + 64, 10);
}

function header(x, y, title, count) {
  const t = text(x, y, title, { size: 20 });
  const w = String(count).length * 8 + 18;
  roundRect(t.x + t.width + 10, y - 17, w, 22, 11, { stroke: GREY });
  text(t.x + t.width + 10 + w / 2, y - 1, String(count), { size: 12, weight: 700, anchor: 'middle' });
}

function stack(cx, cy, s, r) {
  roundRect(cx + s * 0.08, cy, s * 0.92, s * 0.92, r, { stroke: LIGHT, ...WHITE });
  roundRect(cx + s * 0.04, cy + s * 0.04, s * 0.92, s * 0.92, r, { stroke: GREY, ...WHITE });
  roundRect(cx, cy + s * 0.08, s * 0.92, s * 0.92, r, WHITE);
  image(cx, cy + s * 0.08, s * 0.92, s * 0.92, r);
}

function listRow(x, y, { title, author, progress, caption, stacked }) {
  roundRect(x + 16, y, 328, 84, 18);
  if (stacked) stack(x + 26, y + 10, 64, 12);
  else image(x + 26, y + 10, 64, 64, 12);
  text(x + 106, y + 30, title, { size: 15 });
  text(x + 106, y + 48, author, { size: 12, fill: GREY });
  if (progress) {
    line(x + 106, y + 66, x + 106 + 118 * progress, y + 66, { strokeWidth: 4 });
    line(x + 106 + 118 * progress + 4, y + 66, x + 224, y + 66, { stroke: LIGHT, strokeWidth: 4 });
  }
  if (progress) text(x + 332, y + 70, caption, { size: 11, fill: GREY, anchor: 'end' });
  else text(x + 106, y + 70, caption, { size: 11, fill: GREY });
}

function gridTile(cx, top, { title, author, progress, count }) {
  if (count) {
    stack(cx, top, 148, 18);
    roundRect(cx + 100, top + 118, 30, 22, 11, { ...WHITE, strokeWidth: 1.4 });
    text(cx + 115, top + 134, String(count), { size: 12, weight: 700, anchor: 'middle' });
  } else {
    image(cx, top, 148, 148, 18);
  }
  let ty = top + 170;
  if (progress) {
    line(cx, top + 160, cx + 148 * progress, top + 160, { strokeWidth: 4 });
    line(cx + 148 * progress + 4, top + 160, cx + 148, top + 160, { stroke: LIGHT, strokeWidth: 4 });
    ty += 14;
  }
  text(cx, ty, title, { size: 14, weight: 700 });
  text(cx, ty + 18, author, { size: 12, fill: GREY });
  return { x: cx, top };
}

const p = phone(300, 80, 'List');
const { x, y } = p;
topBar(x, y);

text(x + 20, y + 134, 'Good evening', { size: 28 });
text(x + 20, y + 160, '3 books in progress', { size: 14, fill: GREY });

roundRect(x + 16, y + 180, 328, 176, 32);
image(x + 36, y + 200, 88, 88, 14);
text(x + 140, y + 214, 'Continue listening', { size: 12, weight: 700 });
text(x + 140, y + 240, 'Echoes of Tomorrow', { size: 18 });
text(x + 140, y + 262, 'Daniel Hartwell', { size: 13, fill: GREY });
text(x + 140, y + 282, 'The Signal Saga 2', { size: 13, fill: GREY });
line(x + 36, y + 314, x + 200, y + 314, { strokeWidth: 4 });
line(x + 204, y + 314, x + 268, y + 314, { stroke: LIGHT, strokeWidth: 4 });
text(x + 36, y + 338, '72% of book', { size: 12, weight: 700 });
text(x + 268, y + 338, '4:03:15 left', { size: 12, anchor: 'end' });
circle(x + 308, y + 320, 56, HATCH);
icon.play(x + 310, y + 320, 10);

header(x + 20, y + 400, 'Current', 2);
listRow(x, y + 414, { title: 'Stars over Ashford', author: 'Daniel Hartwell', progress: 0.5, caption: '1 of 2 finished', stacked: true });
listRow(x, y + 500, { title: 'Beyond the Horizon', author: 'Alexander Reeves', progress: 0.3, caption: '7:26:05 left' });

header(x + 20, y + 626, 'Not started yet', 12);
listRow(x, y + 642, { title: 'Maple Street Mysteries', author: 'Lena Morgan', caption: '5 books', stacked: true });

note(20, y + 190, ['Continue listening', 'shows the book,', 'with its series', 'and part.']);
arrow([[200, y + 300], [250, y + 300], [x + 10, y + 270]]);

note(20, y + 410, ['A started series is', 'in Current, also', 'between two books.']);
arrow([[200, y + 480], [250, y + 470], [x + 22, y + 456]]);

note(20, y + 650, ['Any other series', 'is one stack.']);
arrow([[200, y + 690], [250, y + 700], [x + 22, y + 684]]);

const q = phone(780, 80, 'Grid, scrolled down');
const gx = q.x;
topBar(gx, y);

header(gx + 20, y + 136, 'Current', 2);
gridTile(gx + 22, y + 152, { title: 'Beyond the Horizon', author: 'Alexander Reeves', progress: 0.3 });
const started = gridTile(gx + 190, y + 152, { title: 'Stars over Ashford', author: 'Daniel Hartwell', progress: 0.5, count: 2 });

header(gx + 20, y + 418, 'Not started yet', 12);
gridTile(gx + 22, y + 434, { title: 'Maple Street Mysteries', author: 'Lena Morgan', count: 5 });
const notStarted = gridTile(gx + 190, y + 434, { title: 'The Lantern Keepers', author: 'Mara Quill', count: 6 });

note(gx + 400, y + 260, ['The bar is the', 'whole series.']);
arrow([[gx + 396, y + 290], [gx + 372, y + 306], [started.x + 156, started.top + 160]]);

note(gx + 400, y + 520, ['A badge counts', 'the books.']);
arrow([[gx + 396, y + 550], [gx + 372, y + 570], [notStarted.x + 136, notStarted.top + 132]]);
