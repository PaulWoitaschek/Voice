const p = phone(300, 80, 'Library grouped by author');
const { x, y } = p;

roundRect(x + 16, y + 40, 274, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);

roundRect(x + 16, y + 106, 150, 34, 17, HATCH);
const group = text(x + 34, y + 128, 'Group: Author', { size: 13, weight: 700 });
icon.chevronDown(x + 148, y + 123, 5);
roundRect(x + 176, y + 106, 120, 34, 17);
text(x + 192, y + 128, 'Sort: Title', { size: 13 });
icon.chevronDown(x + 278, y + 123, 5);

const groups = [
  ['Daniel Hartwell', ['Echoes of Tomorrow', 'Static on the Line', 'Stars over Ashford']],
  ['Lena Morgan', ['The Bookshop on Maple Street', 'The Strange Receipt']],
  ['Mara Quill', ['The Lantern Keepers']],
  ['Unknown author', ['Track 01']],
];
let gy = y + 178;
const headers = groups.map(([author, books]) => {
  const h = text(x + 20, gy, author, { size: 16, weight: 700 });
  text(x + 340, gy, `${books.length}`, { size: 13, fill: GREY, anchor: 'end' });
  gy += 14;
  books.forEach((title) => {
    image(x + 20, gy, 44, 44, 8);
    text(x + 78, gy + 27, title, { size: 14 });
    gy += 54;
  });
  gy += 30;
  return h;
});

note(x + 400, y + 90, ['Pick what to group by:', 'nothing, series or author.']);
arrow([[x + 396, y + 112], [x + 290, y + 158], [group.x + 80, y + 146]]);

note(20, y + 470, ['Many authors', 'have one book, so', 'groups get tiny.']);
arrow([[150, y + 524], [230, y + 560], [headers[2].x - 6, headers[2].y - 6]]);

note(x + 400, y + 640, ['Bad tags end up', 'in their own pile.']);
arrow([[x + 396, y + 656], [x + 300, y + 680], [headers[3].x + headers[3].width + 8, headers[3].y - 6]]);
