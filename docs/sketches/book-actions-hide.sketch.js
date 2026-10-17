const p = phone(300, 80, 'Book actions');
const { x, y } = p;

roundRect(x + 16, y + 40, 250, 48, 24, { stroke: LIGHT });
text(x + 20, y + 134, 'Good evening', { size: 28, fill: LIGHT });
roundRect(x + 16, y + 160, 328, 100, 28, { stroke: LIGHT });

roundRect(x, y + 190, 360, 570, 30, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
line(x + 160, y + 206, x + 200, y + 206, { stroke: GREY, strokeWidth: 4 });
image(x + 24, y + 226, 64, 64, 12);
text(x + 104, y + 248, 'Echoes of Tomorrow', { size: 17 });
const links = run(x + 104, y + 270, [
  { str: 'Daniel Hartwell', style: { weight: 700 } },
  { str: '·', pad: 4, style: { fill: GREY } },
  { str: 'The Signal Saga 2', style: { weight: 700 } },
], { size: 12 });
text(x + 104, y + 288, '72% of book · 4:03:15 left', { size: 12, fill: GREY });

['Not started yet', 'Current', 'Completed'].forEach((status, i) => {
  const tx = x + 24 + i * 108;
  roundRect(tx, y + 312, 96, 64, 18, i === 1 ? HATCH : {});
  circle(tx + 48, y + 334, 18, { stroke: GREY });
  text(tx + 48, y + 364, status, { size: 11, anchor: 'middle', weight: i === 1 ? 700 : 400 });
});

const items = ['Edit', 'Cover from the Internet', 'Cover from the file system', 'Hide from library', 'Select'];
const boxes = items.map((item, i) => {
  const iy = y + 398 + i * 54;
  const hide = item === 'Hide from library';
  if (hide) roundRect(x + 12, iy, 336, 48, 24, HATCH);
  circle(x + 42, iy + 24, 22, { stroke: GREY });
  return text(x + 68, iy + 30, item, { size: 15, weight: hide ? 700 : 400 });
});
const del = { y: y + 682 };
circle(x + 42, del.y + 24, 22);
const deleteText = text(x + 68, del.y + 30, 'Delete Book', { size: 15 });

note(x + 400, y + 220, ['Tap the author or', 'the series to see', 'all their books.']);
arrow([[x + 396, y + 250], [x + 372, y + 262], [links[2].x + links[2].width + 8, links[2].y + 8]]);

note(x + 400, y + 380, ['Replaces Rename: title,', 'author, series and part.']);
arrow([[x + 396, y + 400], [x + 200, y + 400], [boxes[0].x + boxes[0].width + 8, boxes[0].y + 6]]);

note(20, y + 520, ['Out of sight, not', 'deleted. Progress,', 'bookmarks and files', 'stay.']);
arrow([[200, y + 586], [250, y + 584], [x - 4, boxes[3].y + 8]]);

note(x + 400, y + 600, ['Change several', 'books at once.']);
arrow([[x + 396, y + 618], [x + 300, y + 624], [boxes[4].x + boxes[4].width + 8, boxes[4].y + 8]]);

note(x + 400, y + 700, ['Still the only way', 'to delete files.']);
arrow([[x + 396, y + 716], [x + 300, y + 712], [deleteText.x + deleteText.width + 8, deleteText.y + 8]]);
