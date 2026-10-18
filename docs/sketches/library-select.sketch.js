const p = phone(300, 80, 'Select books');
const { x, y } = p;

icon.close(x + 34, y + 50);
text(x + 62, y + 56, '4 selected', { size: 18, weight: 700 });
const selectAll = text(x + 340, y + 56, 'Select all', { size: 14, weight: 700, anchor: 'end' });

const header = text(x + 20, y + 120, 'Not started yet', { size: 20 });
roundRect(header.x + header.width + 10, y + 103, 34, 22, 11, { stroke: GREY });
text(header.x + header.width + 27, y + 119, '46', { size: 12, weight: 700, anchor: 'middle' });

const books = [
  ['The Lantern Keepers 1', true],
  ['The Lantern Keepers 2', true],
  ['The Lantern Keepers 3', true],
  ['The Lantern Keepers 4', true],
  ['The Quiet Orbit', false],
  ['Track 01', false],
];
const rows = books.map(([title, selected], i) => {
  const ry = y + 136 + i * 86;
  roundRect(x + 16, ry, 328, 84, 18, selected ? { strokeWidth: 3 } : { stroke: GREY });
  image(x + 26, ry + 10, 64, 64, 12);
  if (selected) {
    circle(x + 82, ry + 14, 24, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
    icon.check(x + 82, ry + 14, 5);
  }
  text(x + 106, ry + 38, title, { size: 15 });
  text(x + 106, ry + 58, 'Mara Quill', { size: 12, fill: GREY });
  return ry;
});

roundRect(x + 30, y + 676, 300, 56, 28, { ...HATCH, fill: '#d0d0d0', hachureGap: 6 });
['Series', 'Author', 'Hide'].forEach((name, i) => text(x + 80 + i * 100, y + 710, name, { size: 15, weight: 700, anchor: 'middle' }));

note(20, y + 90, ['Select in the book', 'actions, then tap more.']);
arrow([[236, y + 80], [270, y + 62], [x + 16, y + 54]]);

note(x + 400, y + 30, ['Everything shown,', 'like all results', 'of a search.']);
arrow([[x + 396, y + 50], [x + 370, y + 40], [selectAll.x + selectAll.width + 8, selectAll.y + 6]]);

note(x + 400, y + 560, ['Put them in one series,', 'set the author or hide', 'them, all at once.']);
arrow([[x + 396, y + 640], [x + 372, y + 680], [x + 340, y + 700]]);
