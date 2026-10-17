const p = phone(300, 80, 'Library with filter chips');
const { x, y } = p;

roundRect(x + 16, y + 40, 274, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);

roundRect(x + 16, y + 104, 104, 30, 15, HATCH);
const sort = text(x + 30, y + 124, 'Sort: Title', { size: 13, weight: 700 });
icon.chevronDown(x + 106, y + 119, 4);
chip(x + 128, y + 104, 98, 'Not started');
chip(x + 234, y + 104, 74, 'Finished', { selected: true });
roundRect(x + 316, y + 104, 40, 30, 15, { stroke: LIGHT });
text(x + 330, y + 124, 'Ki', { size: 13, fill: LIGHT });

const finished = [
  ['Beyond the Horizon', 'Alexander Reeves'],
  ['The Bookshop on Maple Street', 'Lena Morgan'],
  ['The Compass Chronicles', 'Alexander Reeves · 3 books'],
  ['Static on the Line', 'Daniel Hartwell'],
  ['The Strange Receipt', 'Lena Morgan'],
  ['Stars over Ashford', 'Daniel Hartwell'],
  ['The Lantern Keepers', 'Mara Quill'],
];
finished.forEach(([title, author], i) => {
  const ry = y + 160 + i * 76;
  image(x + 20, ry, 56, 56, 10);
  text(x + 92, ry + 24, title, { size: 15 });
  text(x + 92, ry + 44, author, { size: 12, fill: GREY });
});

note(20, y + 70, ['Always in view,', 'one tap away.']);
arrow([[150, y + 100], [220, y + 130], [sort.x - 18, sort.y - 4]]);

note(x + 410, y + 90, ['Runs off the edge', 'on small phones.']);
arrow([[x + 406, y + 108], [x + 384, y + 114], [x + 364, y + 118]]);

note(x + 400, y + 300, ['Takes over the', 'sections: one list', 'for what you picked.']);
arrow([[x + 396, y + 320], [x + 350, y + 250], [x + 300, y + 192]]);
