const WHITE = { fill: '#fff', fillStyle: 'solid' };

function header(x, y, title, count) {
  const t = text(x, y, title, { size: 20 });
  const w = String(count).length * 7 + 18;
  roundRect(t.x + t.width + 10, y - 17, w, 22, 11, { stroke: GREY });
  text(t.x + t.width + 10 + w / 2, y - 1, String(count), { size: 12, weight: 700, anchor: 'middle' });
}

function stack(cx, cy, s, r) {
  roundRect(cx + s * 0.08, cy, s * 0.92, s * 0.92, r, { stroke: LIGHT, ...WHITE });
  roundRect(cx + s * 0.04, cy + s * 0.04, s * 0.92, s * 0.92, r, { stroke: GREY, ...WHITE });
  roundRect(cx, cy + s * 0.08, s * 0.92, s * 0.92, r, WHITE);
  image(cx, cy + s * 0.08, s * 0.92, s * 0.92, r);
}

function listRow(x, y, { title, author, progress, caption, series }) {
  roundRect(x + 16, y, 328, 84, 18);
  if (series) stack(x + 26, y + 10, 64, 12);
  else image(x + 26, y + 10, 64, 64, 12);
  text(x + 106, y + 30, title, { size: 15 });
  text(x + 106, y + 48, author, { size: 12, fill: GREY });
  if (progress) {
    line(x + 106, y + 66, x + 106 + 118 * progress, y + 66, { strokeWidth: 4 });
    line(x + 106 + 118 * progress + 4, y + 66, x + 224, y + 66, { stroke: LIGHT, strokeWidth: 4 });
    text(x + 332, y + 70, caption, { size: 11, fill: GREY, anchor: 'end' });
  } else {
    text(x + 106, y + 70, caption, { size: 12, fill: GREY });
  }
  return y;
}

const p = phone(300, 80, 'List, sorted by recently added');
const { x, y } = p;

roundRect(x + 16, y + 40, 232, 48, 24);
icon.search(x + 42, y + 64);
text(x + 64, y + 69, 'Search your library', { size: 14, fill: GREY });
icon.sort(x + 278, y + 64);
circle(x + 324, y + 64, 22);
circle(x + 324, y + 64, 8);

header(x + 20, y + 130, 'Current', 1);
listRow(x, y + 144, { title: 'Echoes of Tomorrow', author: 'Daniel Hartwell', progress: 0.72, caption: '4:03:15 left' });

header(x + 20, y + 270, 'All books', '1,204');
label(x + 24, y + 304, 'Today', { bold: true });
listRow(x, y + 316, { title: 'A Map of Small Hours', author: 'Ines Carver', caption: '8:41:12' });
const salt = listRow(x, y + 402, { title: 'Salt and Silence', author: 'Nora Whitfield', caption: '11:20:05' });

label(x + 24, y + 524, 'September', { bold: true });
const lantern = listRow(x, y + 536, { title: 'The Lantern Keepers', author: 'Mara Quill', caption: '6 books', series: true });
listRow(x, y + 622, { title: 'Winter Harbor', author: 'Ruth Calloway', caption: '9:58:30' });

note(20, y + 250, ['The day for the', 'last 30 days,', 'then the month.']);
arrow([[160, y + 330], [230, y + 310], [x + 18, y + 300]]);

note(x + 400, y + 330, ['Books of one day by', 'title, so a folder added', 'at once is easy to scan.']);
arrow([[x + 396, y + 400], [x + 372, y + 420], [x + 350, salt + 40]]);

note(x + 400, y + 560, ['A series goes where', 'its newest book is.']);
arrow([[x + 396, y + 590], [x + 372, y + 586], [x + 350, lantern + 40]]);
