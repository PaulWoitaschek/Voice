const p = phone(300, 80, 'Book actions');
const { x, y } = p;

roundRect(x + 16, y + 40, 274, 48, 24, { stroke: LIGHT });
text(x + 20, y + 140, 'Good morning', { size: 24, fill: LIGHT });
roundRect(x + 16, y + 160, 328, 100, 28, { stroke: LIGHT });

roundRect(x, y + 250, 360, 510, 30, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
line(x + 160, y + 266, x + 200, y + 266, { stroke: GREY, strokeWidth: 4 });
image(x + 24, y + 290, 64, 64, 12);
text(x + 104, y + 316, 'Echoes of Tomorrow', { size: 17 });
text(x + 104, y + 338, 'Daniel Hartwell · The Signal Saga 2', { size: 12, fill: GREY });

const items = ['Edit', 'Cover', 'Mark as Completed', 'Hide from library', 'Delete Book'];
const boxes = items.map((item, i) => {
  const iy = y + 390 + i * 58;
  const hide = i === 3;
  if (hide) roundRect(x + 12, iy, 336, 50, 25, HATCH);
  circle(x + 42, iy + 25, 22, { stroke: i === 4 ? INK : GREY });
  return text(x + 68, iy + 31, item, { size: 15, weight: hide ? 700 : 400 });
});

note(x + 400, y + 360, ['Replaces Rename: title,', 'author and series.']);
arrow([[x + 396, y + 380], [x + 200, y + 380], [boxes[0].x + boxes[0].width + 8, boxes[0].y + 6]]);

note(20, y + 520, ['Out of sight, not', 'deleted. Progress,', 'bookmarks and files', 'stay.']);
arrow([[200, y + 586], [250, y + 600], [x - 4, boxes[3].y + 8]]);

note(x + 400, y + 640, ['Still the only way', 'to delete files.']);
arrow([[x + 396, y + 656], [x + 300, y + 650], [boxes[4].x + boxes[4].width + 8, boxes[4].y + 8]]);
