const WHITE = { fill: '#fff', fillStyle: 'solid' };

function stackedInitial(cx, cy, letter) {
  circle(cx + 6, cy - 4, 36, { stroke: LIGHT, ...WHITE });
  circle(cx + 3, cy - 2, 36, { stroke: GREY, ...WHITE });
  circle(cx, cy, 36, WHITE);
  text(cx, cy + 6, letter, { size: 16, weight: 700, anchor: 'middle' });
}

function bookRow(x, y, { title, details, stacked }) {
  roundRect(x + 16, y, 328, 60, 14);
  const letter = title[0];
  if (stacked) stackedInitial(x + 42, y + 31, letter);
  else {
    circle(x + 42, y + 30, 36);
    text(x + 42, y + 36, letter, { size: 16, weight: 700, anchor: 'middle' });
  }
  text(x + 74, y + 26, title, { size: 15 });
  text(x + 74, y + 45, details, { size: 12, fill: GREY });
}

const p = phone(300, 80, 'Preview');
const { x, y } = p;

icon.back(x + 34, y + 50);
text(x + 180, y + 124, 'We found 8 books', { size: 28, anchor: 'middle' });
const hint = text(x + 180, y + 152, 'Tap one to split or join it.', { size: 14, fill: GREY, anchor: 'middle' });

const books = [
  { title: 'Hobbit', details: '11 hr · 1 audio file' },
  { title: 'Mrs Dalloway', details: 'Woolf · 7 hr · 12 audio files' },
  { title: 'To the Lighthouse', details: 'Woolf · 8 hr · 9 audio files' },
  { title: 'Harry Potter', details: 'Series · Rowling · 2 books', stacked: true },
  { title: 'Die drei ???', details: '141 hr · 160 audio files' },
  { title: 'The Swarm', details: 'Series · 2 books', stacked: true },
];
const rowY = books.map((book, i) => {
  const ry = y + 180 + i * 66;
  bookRow(x, ry, book);
  return ry;
});

button(x + 40, y + 672, 280, 52, 'Add 8 books', { filled: true });

const q = phone(780, 80, 'Tap on Die drei ???');
const sx = q.x;

text(sx + 180, y + 124, 'We found 8 books', { size: 28, anchor: 'middle', fill: LIGHT });
roundRect(sx + 16, y + 180, 328, 60, 14, { stroke: LIGHT });

roundRect(sx, y + 150, 360, 610, 30, { ...WHITE, strokeWidth: 2 });
line(sx + 160, y + 166, sx + 200, y + 166, { stroke: GREY, strokeWidth: 4 });
const title = text(sx + 180, y + 212, 'How is Die drei ???', { size: 22, anchor: 'middle' });
text(sx + 180, y + 240, 'organized?', { size: 22, anchor: 'middle' });

function option(top, { height, selected, badge, lines, summary, count }) {
  roundRect(sx + 16, top, 328, height, 24, selected ? { strokeWidth: 3.2 } : {});
  circle(sx + 46, top + height / 2, 22);
  if (selected) circle(sx + 46, top + height / 2, 10, SOLID);
  let ty = top + 34;
  if (badge) {
    roundRect(sx + 72, top + 18, 124, 24, 12, HATCH);
    text(sx + 134, top + 35, badge, { size: 12, weight: 700, anchor: 'middle' });
    ty = top + 66;
  }
  lines.forEach((l, i) => text(sx + 72, ty + i * 20, l, { size: 16, weight: 700 }));
  ty += lines.length * 20 + 4;
  summary.forEach((l, i) => text(sx + 72, ty + i * 17, l, { size: 12, fill: GREY }));
  ty += summary.length * 17 + 10;
  return text(sx + 72, ty, count, { size: 13, weight: 700 });
}

option(y + 270, {
  height: 146,
  selected: true,
  badge: 'Looks like yours',
  lines: ['Die drei ??? is one book'],
  summary: ['Its files become chapters, even in', 'folders like CD 1 and CD 2.'],
  count: '1 book',
});
const split = option(y + 430, {
  height: 124,
  lines: ['Every folder or file in', 'Die drei ??? is a book'],
  summary: ['Folge 001, Folge 002, Folge 003, …'],
  count: '160 books, a series',
});

note(20, y + 380, ['A series is', 'one row with', 'its books.']);
arrow([[160, y + 400], [230, y + 410], [x + 8, rowY[3] + 30]]);

note(20, y + 620, ['One button,', 'no layouts', 'to pick.']);
arrow([[150, y + 670], [220, y + 700], [x + 34, y + 698]]);

note(sx + 400, y + 170, ['The choice of today,', 'for this one folder.']);
arrow([[sx + 396, y + 190], [sx + 300, y + 186], [title.x + title.width + 6, title.y + 12]]);

note(sx + 400, y + 470, ['Saved for the folder,', 'also when files', 'are added.']);
arrow([[sx + 396, y + 530], [sx + 372, y + 586], [split.x + split.width + 10, split.y + 10]]);
