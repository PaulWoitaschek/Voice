const p = phone(300, 80, 'Select books');
const { x, y } = p;

icon.close(x + 34, y + 50);
const count = text(x + 62, y + 56, '4 selected', { size: 18, weight: 700 });

text(x + 20, y + 120, 'Not started yet', { size: 18 });
const books = [
  ['The Lantern Keepers, Episode 1', true],
  ['The Lantern Keepers, Episode 2', true],
  ['The Lantern Keepers, Episode 3', true],
  ['The Lantern Keepers, Episode 4', true],
  ['The Quiet Orbit', false],
  ['Track 01', false],
];
const rows = books.map(([title, selected], i) => {
  const ry = y + 140 + i * 72;
  if (selected) roundRect(x + 10, ry - 6, 340, 64, 20, { stroke: LIGHT });
  image(x + 22, ry, 52, 52, 10);
  if (selected) {
    circle(x + 66, ry + 6, 24, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
    icon.check(x + 66, ry + 6, 5);
  }
  text(x + 90, ry + 24, title, { size: 14 });
  text(x + 90, ry + 44, 'Mara Quill', { size: 12, fill: GREY });
  return ry;
});

roundRect(x + 30, y + 676, 300, 56, 28, { ...HATCH, fill: '#d0d0d0', hachureGap: 6 });
const actions = ['Series', 'Author', 'Hide'].map((name, i) => text(x + 80 + i * 100, y + 710, name, { size: 15, weight: 700, anchor: 'middle' }));

note(20, y + 30, ['Select in the book', 'actions, then tap more.']);
arrow([[150, y + 80], [220, y + 60], [x + 14, y + 52]]);

note(x + 400, y + 560, ['Put them in one series,', 'set the author or hide', 'them, all at once.']);
arrow([[x + 396, y + 620], [x + 300, y + 650], [actions[0].x + actions[0].width / 2, actions[0].y - 6]]);

note(20, y + 300, ['Parts follow', 'the titles.']);
arrow([[140, y + 330], [220, y + 340], [x + 4, rows[2] + 26]]);
