const WHITE = { fill: '#fff', fillStyle: 'solid' };

function stack(cx, cy, s, r) {
  roundRect(cx + s * 0.08, cy, s * 0.92, s * 0.92, r, { stroke: LIGHT, ...WHITE });
  roundRect(cx + s * 0.04, cy + s * 0.04, s * 0.92, s * 0.92, r, { stroke: GREY, ...WHITE });
  roundRect(cx, cy + s * 0.08, s * 0.92, s * 0.92, r, WHITE);
  image(cx, cy + s * 0.08, s * 0.92, s * 0.92, r);
}

function row(x, ry, title, subtitle) {
  roundRect(x + 16, ry, 328, 72, 14, { stroke: GREY });
  text(x + 98, ry + 32, title, { size: 15 });
  return text(x + 98, ry + 52, subtitle, { size: 12, fill: GREY });
}

const p = phone(300, 80, 'Search');
const { x, y } = p;

roundRect(x + 16, y + 40, 328, 52, 26);
icon.back(x + 42, y + 66);
text(x + 66, y + 72, 'hartwell', { size: 16 });
icon.close(x + 318, y + 66, 7);

chip(x + 16, y + 108, 84, 'All  7', { selected: true });
chip(x + 108, y + 108, 112, 'Authors  7');

roundRect(x + 16, y + 154, 328, 150, 28, { strokeWidth: 2 });
image(x + 28, y + 166, 96, 96, 14);
text(x + 140, y + 190, 'Echoes of Tomorrow', { size: 18 });
text(x + 140, y + 212, 'Daniel Hartwell', { size: 13, fill: GREY });
text(x + 140, y + 232, 'Series · The Signal Saga 2', { size: 12, fill: GREY });
line(x + 140, y + 262, x + 220, y + 262, { strokeWidth: 4 });
line(x + 224, y + 262, x + 262, y + 262, { stroke: LIGHT, strokeWidth: 4 });
text(x + 140, y + 286, '4:03:15 left', { size: 12 });
circle(x + 304, y + 270, 52, HATCH);
icon.play(x + 306, y + 270, 9);

const author = y + 318;
row(x, author, 'Daniel Hartwell', 'Author · 7 books');
circle(x + 58, author + 36, 52, { stroke: GREY });
circle(x + 58, author + 30, 14);
path(`M${x + 46},${author + 50} A12,9 0 0 1 ${x + 70},${author + 50}`);
icon.chevronRight(x + 322, author + 36);

[['The Signal Saga', 'Series · 4 books'], ['Stars over Ashford', 'Series · 2 books']].forEach(([title, subtitle], i) => {
  const ry = author + 74 + i * 74;
  row(x, ry, title, subtitle);
  stack(x + 30, ry + 8, 56, 10);
  icon.chevronRight(x + 322, ry + 36);
});

const hidden = author + 232;
row(x, hidden, 'The Last Broadcast', 'Daniel Hartwell');
image(x + 26, hidden + 8, 56, 56, 10);
roundRect(x + 260, hidden + 24, 70, 24, 12, { stroke: GREY });
text(x + 295, hidden + 41, 'Hidden', { size: 12, anchor: 'middle' });

const book = hidden + 74;
row(x, book, 'Static on the Line', 'Series · The Signal Saga 1');
image(x + 26, book + 8, 56, 56, 10);

note(20, y + 300, ['Authors and series', 'that match open', 'their screen.']);
arrow([[200, y + 340], [250, y + 370], [x + 10, author + 40]]);

note(x + 400, y + 520, ['Hidden books are', 'found too, and', 'marked.']);
arrow([[x + 396, y + 560], [x + 372, hidden + 50], [x + 338, hidden + 38]]);

note(20, y + 620, ['Books of a series', 'are still found', 'one by one.']);
arrow([[200, y + 660], [250, y + 670], [x + 10, book + 40]]);
