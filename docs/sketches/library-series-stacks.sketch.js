const p = phone(300, 80, 'Library with series');
const { x, y } = p;
const WHITE = { fill: '#fff', fillStyle: 'solid' };

function cover(cx, cy, s, stacked) {
  if (stacked) {
    roundRect(cx + 8, cy - 8, s, s, 12, { stroke: LIGHT, ...WHITE });
    roundRect(cx + 4, cy - 4, s, s, 12, { stroke: GREY, ...WHITE });
  }
  roundRect(cx, cy, s, s, 12, WHITE);
  image(cx, cy, s, s, 12);
}

roundRect(x + 16, y + 40, 274, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);

roundRect(x + 16, y + 108, 328, 112, 28);
image(x + 32, y + 124, 80, 80, 12);
text(x + 128, y + 138, 'Continue listening', { size: 12, fill: GREY });
text(x + 128, y + 160, 'Echoes of Tomorrow', { size: 15, weight: 700 });
text(x + 128, y + 178, 'The Signal Saga 2', { size: 12, fill: GREY });
line(x + 128, y + 200, x + 210, y + 200, { strokeWidth: 4 });
line(x + 214, y + 200, x + 268, y + 200, { stroke: LIGHT, strokeWidth: 4 });
circle(x + 306, y + 186, 48, HATCH);
icon.play(x + 308, y + 186, 9);

text(x + 20, y + 262, 'Current', { size: 18 });
const current = [
  ['Stars over Ashford', '1 of 2 finished', true],
  ['Beyond the Horizon', '7:26:05 left', false],
].map(([title, subtitle, stacked], i) => {
  const cx = x + 20 + i * 168;
  const top = stacked ? y + 290 : y + 278;
  if (stacked) {
    roundRect(cx + 12, y + 278, 140, 108, 18, { stroke: LIGHT, ...WHITE });
    roundRect(cx + 6, y + 284, 140, 108, 18, { stroke: GREY, ...WHITE });
  }
  const w = stacked ? 140 : 152;
  const h = stacked ? 108 : 120;
  roundRect(cx, top, w, h, 18, WHITE);
  image(cx, top, w, h, 18);
  text(cx, y + 418, title, { size: 14, weight: 700 });
  const sub = text(cx, y + 436, subtitle, { size: 12, fill: GREY });
  return { cx, top, sub };
});

text(x + 20, y + 488, 'Not started yet', { size: 18 });
const rows = [
  ['Maple Street Mysteries', 'Lena Morgan · 5 books', true],
  ['The Quiet Orbit', 'Mara Quill · 9:12:40', false],
  ['The Lantern Keepers', 'Mara Quill · 6 books', true],
].map(([title, subtitle, stacked], i) => {
  const ry = y + 512 + i * 72;
  cover(x + 20, ry, 52, stacked);
  text(x + 92, ry + 24, title, { size: 15 });
  text(x + 92, ry + 44, subtitle, { size: 12, fill: GREY });
  return ry;
});

note(20, y + 110, ['The book you are in', 'shows on its own,', 'with its part.']);
arrow([[150, y + 172], [220, y + 200], [x - 4, y + 180]]);

note(20, y + 300, ['A started series is', 'in progress, also', 'between two books.']);
arrow([[200, y + 350], [250, y + 370], [current[0].cx - 6, current[0].top + 40]]);

note(20, y + 560, ['Any other series', 'is one stack.']);
arrow([[140, y + 590], [220, y + 560], [x + 12, rows[0] + 30]]);
