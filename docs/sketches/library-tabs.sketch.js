const p = phone(300, 80, 'Library with tabs');
const { x, y } = p;
const WHITE = { fill: '#fff', fillStyle: 'solid' };

function stack(sx, sy, s) {
  roundRect(sx + 8, sy - 8, s, s, 12, { stroke: LIGHT, ...WHITE });
  roundRect(sx + 4, sy - 4, s, s, 12, { stroke: GREY, ...WHITE });
  roundRect(sx, sy, s, s, 12, WHITE);
  image(sx, sy, s, s, 12);
}

roundRect(x + 16, y + 40, 274, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);

const segments = ['Books', 'Series', 'Authors'].map((name, i) => {
  const sx = x + 16 + i * 112;
  roundRect(sx, y + 112, 104, 44, 22, i === 1 ? HATCH : {});
  text(sx + 52, y + 139, name, { size: 14, weight: i === 1 ? 700 : 400, anchor: 'middle' });
  return sx;
});

const rows = [
  ['The Signal Saga', 'Daniel Hartwell', '4 books · 1 finished', 0.3],
  ['Maple Street Mysteries', 'Lena Morgan', '5 books', 0],
  ['The Compass Chronicles', 'Alexander Reeves', '3 books · 3 finished', 1],
  ['Stars over Ashford', 'Daniel Hartwell', '2 books · 1 in progress', 0.2],
  ['The Lantern Keepers', 'Mara Quill', '6 books', 0],
];
const boxes = rows.map(([title, author, count, progress], i) => {
  const ry = y + 190 + i * 104;
  stack(x + 20, ry, 72);
  const t = text(x + 112, ry + 20, title, { size: 15, weight: 700 });
  text(x + 112, ry + 40, author, { size: 12, fill: GREY });
  const c = text(x + 112, ry + 58, count, { size: 12, fill: GREY });
  const end = x + 112 + 200 * progress;
  if (progress > 0) line(x + 112, ry + 74, end, ry + 74, { strokeWidth: 4 });
  if (progress < 1) line(Math.max(end + 4, x + 112), ry + 74, x + 312, ry + 74, { stroke: LIGHT, strokeWidth: 4 });
  return { t, c, ry };
});

note(20, y + 120, ['Three views of', 'the same books.']);
arrow([[150, y + 150], [230, y + 160], [segments[0] - 6, y + 134]]);

note(x + 400, y + 100, ['Authors only work', 'with clean tags, or', '"J. K. Rowling"', 'shows up twice.']);
arrow([[x + 396, y + 120], [x + 360, y + 96], [segments[2] + 90, y + 108]]);

note(20, y + 420, ['Each series shows', 'its author and how', 'far you are in it.']);
arrow([[150, y + 474], [240, y + 520], [x + 14, boxes[3].ry + 40]]);
