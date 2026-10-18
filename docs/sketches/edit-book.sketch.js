const p = phone(300, 80, 'Edit');
const { x, y } = p;
const WHITE = { fill: '#fff', fillStyle: 'solid' };

function field(fx, fy, w, name, value) {
  roundRect(fx, fy, w, 56, 12);
  rect(fx + 12, fy - 8, name.length * 7 + 8, 14, { stroke: 'none', ...WHITE });
  text(fx + 16, fy + 4, name, { size: 11, fill: GREY });
  return text(fx + 16, fy + 35, value, { size: 15 });
}

roundRect(x, y + 120, 360, 640, 30, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
line(x + 160, y + 136, x + 200, y + 136, { stroke: GREY, strokeWidth: 4 });
image(x + 24, y + 160, 64, 64, 12);
text(x + 104, y + 186, 'Echoes of Tomorrow', { size: 17 });
text(x + 104, y + 208, 'Daniel Hartwell', { size: 12, fill: GREY });

field(x + 24, y + 256, 312, 'Title', 'Echoes of Tomorrow');
const author = field(x + 24, y + 334, 312, 'Author', 'Daniel Hartwell');
const series = field(x + 24, y + 412, 220, 'Series', 'The Sig');
line(series.x + series.width + 3, series.y - 2, series.x + series.width + 3, series.y + series.height + 2, { strokeWidth: 1.4 });
const part = field(x + 256, y + 412, 80, 'Part', '2');

roundRect(x + 24, y + 474, 220, 100, 12, { ...WHITE, strokeWidth: 1.4 });
const suggestion = text(x + 40, y + 504, 'The Signal Saga', { size: 14, weight: 700 });
text(x + 40, y + 522, '4 books', { size: 12, fill: GREY });
text(x + 40, y + 554, 'New series “The Sig”', { size: 14, fill: GREY });

const reset = text(x + 24, y + 640, 'Use the names from the files', { size: 14, weight: 700, fill: GREY });
roundRect(x + 236, y + 612, 100, 48, 24, HATCH);
text(x + 286, y + 642, 'Save', { size: 14, weight: 700, anchor: 'middle' });

note(x + 400, y + 300, ['Fix the author without', 'touching the files.']);
arrow([[x + 396, y + 320], [x + 360, y + 340], [author.x + author.width + 12, author.y - 6]]);

note(20, y + 470, ['Pick a series you', 'have, so it isn\'t', 'there twice.']);
arrow([[150, y + 524], [240, y + 540], [x + 18, y + 510]]);

note(x + 400, y + 420, ['The part sets the', 'order in the series.']);
arrow([[x + 396, y + 440], [x + 370, y + 450], [part.x + 40, part.y - 30]]);

note(20, y + 640, ['Undo all edits', 'in one tap.']);
arrow([[150, y + 664], [230, y + 690], [reset.x + 60, reset.y + reset.height + 4]]);
