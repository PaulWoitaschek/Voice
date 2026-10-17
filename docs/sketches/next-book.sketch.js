const WHITE = { fill: '#fff', fillStyle: 'solid' };

const p = phone(300, 80, 'The book ends');
const { x, y } = p;

iconButton(x + 42, y + 50, 'chevronDown');
iconButton(x + 268, y + 50, 'chapters');
iconButton(x + 318, y + 50, 'more');
image(x + 80, y + 92, 200, 200, 22);
text(x + 30, y + 334, 'Echoes of Tomorrow', { size: 23 });
text(x + 30, y + 358, 'Daniel Hartwell', { size: 15, fill: GREY });

bookBar(x + 30, y + 392, 300, { progress: 1 });
text(x + 30, y + 420, '100% of book', { size: 13 });
text(x + 330, y + 420, 'Finished', { size: 13, fill: GREY, anchor: 'end' });

const card = { x: x + 30, y: y + 448, w: 300, h: 96 };
roundRect(card.x, card.y, card.w, card.h, 26);
image(x + 46, y + 464, 64, 64, 12);
text(x + 126, y + 476, 'Up next', { size: 12, fill: GREY });
text(x + 126, y + 498, 'Interference', { size: 15, weight: 700 });
text(x + 126, y + 518, 'The Signal Saga 3', { size: 12, fill: GREY });
circle(x + 290, y + 496, 48, HATCH);
icon.play(x + 292, y + 496, 9);

iconButton(x + 88, y + 620, 'rewind', { d: 66 });
roundRect(x + 134, y + 574, 92, 92, 30, HATCH);
icon.play(x + 182, y + 620, 15);
iconButton(x + 272, y + 620, 'forward', { d: 66 });

toolbar(x + 46, y + 682, 268, ['moon', 'speed', 'bookmark', 'skipSilence', 'volume']);

note(20, y + 420, ['When a book ends,', 'the next one in the', 'series is offered.']);
arrow([[150, y + 500], [220, y + 520], [card.x - 8, card.y + 50]]);

note(20, y + 600, ['It never starts on', 'its own, so a book', 'that ends at night', 'stays quiet.']);

const q = phone(780, 80, 'Library');
const lx = q.x;

roundRect(lx + 16, y + 40, 250, 48, 24);
icon.search(lx + 42, y + 64);
text(lx + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(lx + 290, y + 64, 40);
roundRect(lx + 281, y + 57, 18, 14, 3);
circle(lx + 330, y + 64, 40);
circle(lx + 330, y + 64, 10);

text(lx + 20, y + 134, 'Good evening', { size: 28 });
text(lx + 20, y + 160, '2 books in progress', { size: 14, fill: GREY });

const hero = { x: lx + 16, y: y + 180, w: 328, h: 176 };
roundRect(hero.x, hero.y, hero.w, hero.h, 32);
image(lx + 36, y + 200, 88, 88, 14);
text(lx + 140, y + 214, 'Up next', { size: 12, weight: 700 });
text(lx + 140, y + 240, 'Interference', { size: 18 });
text(lx + 140, y + 262, 'Daniel Hartwell', { size: 13, fill: GREY });
text(lx + 140, y + 282, 'The Signal Saga 3', { size: 13, fill: GREY });
line(lx + 36, y + 314, lx + 268, y + 314, { stroke: LIGHT, strokeWidth: 4 });
text(lx + 36, y + 338, '0% of book', { size: 12, weight: 700 });
text(lx + 268, y + 338, '11:02:18 left', { size: 12, anchor: 'end' });
circle(lx + 308, y + 320, 56, HATCH);
icon.play(lx + 310, y + 320, 10);

const current = text(lx + 20, y + 400, 'Current', { size: 20 });
roundRect(current.x + current.width + 10, y + 383, 26, 22, 11, { stroke: GREY });
text(current.x + current.width + 23, y + 399, '2', { size: 12, weight: 700, anchor: 'middle' });
[
  ['Stars over Ashford', 'Daniel Hartwell', 0.5, '1 of 2 finished', true],
  ['Beyond the Horizon', 'Alexander Reeves', 0.3, '7:26:05 left', false],
].forEach(([title, author, progress, caption, stacked], i) => {
  const ry = y + 414 + i * 86;
  roundRect(lx + 16, ry, 328, 84, 18);
  if (stacked) {
    roundRect(lx + 31, ry + 10, 59, 59, 12, { stroke: LIGHT, ...WHITE });
    roundRect(lx + 28, ry + 13, 59, 59, 12, { stroke: GREY, ...WHITE });
    roundRect(lx + 26, ry + 15, 59, 59, 12, WHITE);
    image(lx + 26, ry + 15, 59, 59, 12);
  } else {
    image(lx + 26, ry + 10, 64, 64, 12);
  }
  text(lx + 106, ry + 30, title, { size: 15 });
  text(lx + 106, ry + 48, author, { size: 12, fill: GREY });
  line(lx + 106, ry + 66, lx + 106 + 118 * progress, ry + 66, { strokeWidth: 4 });
  line(lx + 110 + 118 * progress, ry + 66, lx + 224, ry + 66, { stroke: LIGHT, strokeWidth: 4 });
  text(lx + 332, ry + 70, caption, { size: 11, fill: GREY, anchor: 'end' });
});

note(lx + 400, y + 220, ['Continue listening', 'offers it too.']);
arrow([[lx + 392, y + 250], [lx + 376, y + 262], [hero.x + hero.w + 8, hero.y + 92]]);
