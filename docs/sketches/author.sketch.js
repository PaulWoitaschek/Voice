const p = phone(300, 80, 'Author');
const { x, y } = p;
const WHITE = { fill: '#fff', fillStyle: 'solid' };

icon.back(x + 34, y + 50);
const more = { x: x + 324, y: y + 50 };
icon.more(more.x, more.y);

const name = text(x + 20, y + 130, 'Daniel Hartwell', { size: 28 });
text(x + 20, y + 156, '2 series · 7 books · 2 finished', { size: 13, fill: GREY });

const rows = [
  ['The Signal Saga', '4 books', 0.4, '1 of 4 finished', true],
  ['Stars over Ashford', '2 books', 0.5, '1 of 2 finished', true],
  ['The Last Broadcast', null, 0, '9:12:40 left', false],
].map(([title, subtitle, progress, caption, stacked], i) => {
  const ry = y + 188 + i * 86;
  roundRect(x + 16, ry, 328, 84, 18);
  if (stacked) {
    roundRect(x + 31, ry + 10, 59, 59, 12, { stroke: LIGHT, ...WHITE });
    roundRect(x + 28, ry + 13, 59, 59, 12, { stroke: GREY, ...WHITE });
    roundRect(x + 26, ry + 15, 59, 59, 12, WHITE);
    image(x + 26, ry + 15, 59, 59, 12);
  } else {
    image(x + 26, ry + 10, 64, 64, 12);
  }
  text(x + 106, ry + (subtitle ? 30 : 38), title, { size: 15 });
  if (subtitle) text(x + 106, ry + 48, subtitle, { size: 12, fill: GREY });
  if (progress) {
    line(x + 106, ry + 66, x + 106 + 118 * progress, ry + 66, { strokeWidth: 4 });
    line(x + 110 + 118 * progress, ry + 66, x + 224, ry + 66, { stroke: LIGHT, strokeWidth: 4 });
    text(x + 332, ry + 70, caption, { size: 11, fill: GREY, anchor: 'end' });
  } else {
    text(x + 106, ry + 58, caption, { size: 11, fill: GREY });
  }
  return ry;
});

note(20, y + 60, ['Tap the author', 'of any book.']);
arrow([[200, y + 100], [250, y + 120], [name.x - 8, name.y + 10]]);

note(x + 400, y + 20, ['Fix the spelling', 'for all their books.']);
arrow([[x + 396, y + 40], [x + 370, y + 30], [more.x + 10, more.y - 14]]);

note(x + 400, y + 220, ['Series first, as', 'stacks, then the', 'books on their own.']);
arrow([[x + 396, y + 240], [x + 372, y + 226], [x + 350, rows[0] + 40]]);
