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

roundRect(lx + 16, y + 40, 274, 48, 24);
icon.search(lx + 42, y + 64);
text(lx + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(lx + 324, y + 64, 22);
circle(lx + 324, y + 64, 8);

const hero = { x: lx + 16, y: y + 108, w: 328, h: 112 };
roundRect(hero.x, hero.y, hero.w, hero.h, 28);
image(lx + 32, y + 124, 80, 80, 12);
text(lx + 128, y + 146, 'Up next', { size: 12, fill: GREY });
text(lx + 128, y + 168, 'Interference', { size: 15, weight: 700 });
text(lx + 128, y + 188, 'The Signal Saga 3', { size: 12, fill: GREY });
circle(lx + 306, y + 164, 48, HATCH);
icon.play(lx + 308, y + 164, 9);

text(lx + 20, y + 262, 'Current', { size: 18 });
roundRect(lx + 32, y + 278, 140, 108, 18, { stroke: LIGHT, ...WHITE });
roundRect(lx + 26, y + 284, 140, 108, 18, { stroke: GREY, ...WHITE });
roundRect(lx + 20, y + 290, 140, 108, 18, WHITE);
image(lx + 20, y + 290, 140, 108, 18);
text(lx + 20, y + 418, 'Stars over Ashford', { size: 14, weight: 700 });
text(lx + 20, y + 436, '1 of 2 finished', { size: 12, fill: GREY });
image(lx + 188, y + 278, 152, 120, 18);
text(lx + 188, y + 418, 'Beyond the Horizon', { size: 14, weight: 700 });
text(lx + 188, y + 436, '7:26:05 left', { size: 12, fill: GREY });

text(lx + 20, y + 488, 'Not started yet', { size: 18 });
[
  ['Maple Street Mysteries', 'Lena Morgan · 5 books'],
  ['The Quiet Orbit', 'Mara Quill · 9:12:40'],
  ['The Lantern Keepers', 'Mara Quill · 6 books'],
].forEach(([title, subtitle], i) => {
  const ry = y + 512 + i * 72;
  image(lx + 20, ry, 52, 52, 12);
  text(lx + 92, ry + 24, title, { size: 15 });
  text(lx + 92, ry + 44, subtitle, { size: 12, fill: GREY });
});

note(lx + 400, y + 220, ['Continue listening', 'offers it too.']);
arrow([[lx + 392, y + 236], [lx + 374, y + 214], [hero.x + hero.w + 8, hero.y + 84]]);
