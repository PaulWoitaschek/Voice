const WHITE = { fill: '#fff', fillStyle: 'solid' };

const p = phone(300, 80, 'Book actions');
const { x, y } = p;

roundRect(x + 16, y + 40, 250, 48, 24, { stroke: LIGHT });
text(x + 20, y + 134, 'Good evening', { size: 28, fill: LIGHT });

roundRect(x, y + 160, 360, 600, 30, { ...WHITE, strokeWidth: 2 });
line(x + 160, y + 176, x + 200, y + 176, { stroke: GREY, strokeWidth: 4 });
image(x + 24, y + 196, 64, 64, 12);
text(x + 104, y + 218, 'Die drei ???', { size: 17 });
text(x + 104, y + 240, '160 chapters', { size: 12, fill: GREY });
text(x + 104, y + 258, '3% of book · 137:10:24 left', { size: 12, fill: GREY });

['Not started yet', 'Current', 'Completed'].forEach((status, i) => {
  const tx = x + 24 + i * 108;
  roundRect(tx, y + 282, 96, 64, 18, i === 1 ? HATCH : {});
  circle(tx + 48, y + 304, 18, { stroke: GREY });
  text(tx + 48, y + 334, status, { size: 11, anchor: 'middle', weight: i === 1 ? 700 : 400 });
});

const items = ['Edit', 'Cover from the Internet', 'Cover from the file system', 'Split into 160 books', 'Hide from library', 'Select', 'Delete Book'];
const boxes = items.map((item, i) => {
  const iy = y + 360 + i * 54;
  const split = item.startsWith('Split');
  if (split) roundRect(x + 12, iy, 336, 48, 24, HATCH);
  circle(x + 42, iy + 24, 22, { stroke: split ? INK : GREY });
  return text(x + 68, iy + 30, item, { size: 15, weight: split ? 700 : 400 });
});

note(20, y + 470, ['For a book with', 'more than one', 'folder or file.', 'Each becomes a', 'book of a series.']);
arrow([[190, y + 560], [240, y + 548], [x - 4, boxes[3].y + 8]]);

const q = phone(840, 80, 'Series, more options');
const sx = q.x;

icon.back(sx + 34, y + 50);
icon.more(sx + 324, y + 50);

roundRect(sx + 36, y + 84, 120, 120, 18, { stroke: LIGHT, ...WHITE });
roundRect(sx + 28, y + 92, 120, 120, 18, { stroke: GREY, ...WHITE });
roundRect(sx + 20, y + 100, 120, 120, 18, WHITE);
image(sx + 20, y + 100, 120, 120, 18);
text(sx + 174, y + 150, 'The Swarm', { size: 22 });
text(sx + 174, y + 176, 'Frank Schätzing', { size: 13, fill: GREY });
text(sx + 174, y + 196, '2 books', { size: 12, fill: GREY });

roundRect(sx + 20, y + 244, 320, 52, 26, HATCH);
icon.play(sx + 132, y + 270, 8);
text(sx + 150, y + 275, 'Start book 1', { size: 14, weight: 700 });

[
  ['1', 'Part 1 - The Beginning', '9:12:40'],
  ['2', 'Part 2 - The Escape', '10:05:12'],
].forEach(([part, title, sub], i) => {
  const ry = y + 326 + i * 92;
  roundRect(sx + 16, ry, 328, 80, 20);
  text(sx + 38, ry + 48, part, { size: 22, weight: 700, anchor: 'middle' });
  image(sx + 60, ry + 14, 52, 52, 10);
  text(sx + 126, ry + 34, title, { size: 15 });
  text(sx + 126, ry + 54, sub, { size: 12, fill: GREY });
});

roundRect(sx + 150, y + 70, 196, 164, 16, { ...WHITE, strokeWidth: 1.8 });
const menu = ['Rename', 'Hide', 'Join into one book'].map((item, i) => {
  const iy = y + 80 + i * 50;
  const join = item.startsWith('Join');
  if (join) roundRect(sx + 158, iy, 180, 44, 14, HATCH);
  return text(sx + 174, iy + 28, item, { size: 15, weight: join ? 700 : 400 });
});

note(sx + 400, y + 160, ['For a series that', 'is one folder: its', 'books become one', 'book again.']);
arrow([[sx + 396, y + 190], [sx + 370, y + 200], [menu[2].x + menu[2].width + 30, menu[2].y + 8]]);

