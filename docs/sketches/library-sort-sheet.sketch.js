const p = phone(300, 80, 'Sort and show');
const { x, y } = p;

function toggle(tx, ty, on) {
  roundRect(tx, ty, 48, 28, 14, on ? HATCH : {});
  circle(on ? tx + 34 : tx + 14, ty + 14, 18, on ? SOLID : {});
}

roundRect(x + 16, y + 40, 232, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
const sort = { x: x + 278, y: y + 64 };
icon.sort(sort.x, sort.y);
circle(sort.x + 15, sort.y - 12, 7, SOLID);
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);
text(x + 20, y + 140, 'Good morning', { size: 24, fill: LIGHT });
roundRect(x + 16, y + 160, 328, 100, 28, { stroke: LIGHT });

roundRect(x, y + 280, 360, 480, 30, { fill: '#fff', fillStyle: 'solid', strokeWidth: 2 });
line(x + 160, y + 296, x + 200, y + 296, { stroke: GREY, strokeWidth: 4 });

label(x + 24, y + 336, 'Sort by', { bold: true });
chip(x + 24, y + 350, 96, 'Progress', { selected: true });
chip(x + 128, y + 350, 64, 'Title');
chip(x + 200, y + 350, 76, 'Author');
chip(x + 24, y + 388, 130, 'Recently added');

label(x + 24, y + 450, 'Show', { bold: true });
text(x + 24, y + 488, 'Finished books', { size: 15 });
toggle(x + 290, y + 468, true);
const hidden = text(x + 24, y + 534, 'Hidden books', { size: 15 });
text(x + 24, y + 552, '12 books', { size: 12, fill: GREY });
toggle(x + 290, y + 518, false);

const from = label(x + 24, y + 604, 'From', { bold: true });
chip(x + 24, y + 618, 120, 'Audiobooks', { selected: true });
chip(x + 152, y + 618, 70, 'Kids');
chip(x + 24, y + 658, 210, 'audiobooks.example.com', { selected: true });

note(x + 400, y + 20, ['One place for the order', 'and what you see. The dot', 'says a filter is on.']);
arrow([[x + 396, y + 44], [x + 320, y + 20], [sort.x + 4, sort.y - 22]]);

note(x + 400, y + 330, ['Like today until you change', 'it. The others show all', 'books in one list.']);
arrow([[x + 396, y + 350], [x + 340, y + 340], [x + 282, y + 362]]);

note(20, y + 464, ['Hidden books', 'come back here.']);
arrow([[150, y + 494], [220, y + 534], [hidden.x - 8, hidden.y - 6]]);

note(x + 400, y + 604, ['Only shows up with', 'more than one folder', 'or a server.']);
arrow([[x + 396, y + 624], [x + 330, y + 634], [x + 240, y + 636]]);
