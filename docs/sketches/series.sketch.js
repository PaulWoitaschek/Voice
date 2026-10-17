const p = phone(300, 80, 'Series');
const { x, y } = p;
const WHITE = { fill: '#fff', fillStyle: 'solid' };

icon.back(x + 34, y + 50);
const more = { x: x + 324, y: y + 50 };
icon.more(more.x, more.y);

roundRect(x + 36, y + 84, 120, 120, 18, { stroke: LIGHT, ...WHITE });
roundRect(x + 28, y + 92, 120, 120, 18, { stroke: GREY, ...WHITE });
roundRect(x + 20, y + 100, 120, 120, 18, WHITE);
image(x + 20, y + 100, 120, 120, 18);
text(x + 174, y + 130, 'The Signal', { size: 22 });
text(x + 174, y + 156, 'Saga', { size: 22 });
text(x + 174, y + 180, 'Daniel Hartwell', { size: 13, fill: GREY });
text(x + 174, y + 200, '4 books · 1 finished', { size: 12, fill: GREY });

roundRect(x + 20, y + 244, 320, 52, 26, HATCH);
icon.play(x + 128, y + 270, 8);
text(x + 146, y + 275, 'Continue book 2', { size: 14, weight: 700 });

const books = [
  ['1', 'Static on the Line', 'Finished', 1],
  ['2', 'Echoes of Tomorrow', '72% · 4:14:24 left', 0.72],
  ['3', 'Interference', '11:02:18', 0],
  ['4', "Mira's Theory", '9:45:51', 0],
];
const rows = books.map(([part, title, sub, progress], i) => {
  const ry = y + 326 + i * 92;
  roundRect(x + 16, ry, 328, 80, 20);
  const n = text(x + 38, ry + 48, part, { size: 22, weight: 700, anchor: 'middle' });
  image(x + 60, ry + 14, 52, 52, 10);
  text(x + 126, ry + 34, title, { size: 15 });
  text(x + 126, ry + 54, sub, { size: 12, fill: GREY });
  if (progress === 1) icon.check(x + 318, ry + 40);
  if (progress > 0 && progress < 1) {
    line(x + 126, ry + 66, x + 126 + 190 * progress, ry + 66, { strokeWidth: 3 });
    line(x + 130 + 190 * progress, ry + 66, x + 316, ry + 66, { stroke: LIGHT, strokeWidth: 3 });
  }
  return { n, ry };
});

note(x + 400, y + 30, ['Rename or hide', 'the series.']);
arrow([[x + 396, y + 44], [x + 370, y + 30], [more.x + 10, more.y - 14]]);

note(x + 400, y + 250, ['Picks up the next', 'book in the series,', 'no searching.']);
arrow([[x + 396, y + 270], [x + 370, y + 256], [x + 344, y + 270]]);

note(20, y + 420, ['Always in the', "series' order,", 'by part.']);
arrow([[140, y + 474], [220, y + 500], [rows[1].n.x - 18, rows[1].ry + 40]]);
