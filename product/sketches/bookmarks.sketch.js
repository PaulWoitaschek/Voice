// The player without bookmark pins, and the bookmarks screen where they move to.
const p = phone(260, 80, 'Player');
iconButton(p.x + 42, p.y + 50, 'chevronDown');
iconButton(p.x + 268, p.y + 50, 'chapters');
iconButton(p.x + 318, p.y + 50, 'more');
image(p.x + 80, p.y + 92, 200, 200, 22);
text(p.x + 30, p.y + 334, 'Echoes of Tomorrow', { size: 23 });
text(p.x + 30, p.y + 358, 'Daniel Hartwell', { size: 15, fill: GREY });
bookBar(p.x + 30, p.y + 392, 300, { progress: 0.72 });
text(p.x + 30, p.y + 420, '72% of book', { size: 13 });
text(p.x + 330, p.y + 420, '4:14:24 left', { size: 13, fill: GREY, anchor: 'end' });
iconButton(p.x + 52, p.y + 470, 'previous', { d: 48 });
roundRect(p.x + 84, p.y + 446, 192, 48, 24);
text(p.x + 104, p.y + 465, 'Chapter 12 of 18', { size: 11, fill: GREY });
text(p.x + 104, p.y + 484, 'The Signal', { size: 15 });
icon.chevronDown(p.x + 254, p.y + 470, 6);
iconButton(p.x + 308, p.y + 470, 'next', { d: 48 });
slider(p.x + 34, p.y + 530, 292, 0.4);
text(p.x + 32, p.y + 562, '14:48', { size: 13, fill: GREY });
text(p.x + 328, p.y + 562, '-21:12', { size: 13, fill: GREY, anchor: 'end' });
iconButton(p.x + 88, p.y + 620, 'rewind', { d: 66 });
roundRect(p.x + 134, p.y + 574, 92, 92, 30, HATCH);
icon.play(p.x + 182, p.y + 620, 15);
iconButton(p.x + 272, p.y + 620, 'forward', { d: 66 });
const [, , bookmarkButton] = toolbar(p.x + 46, p.y + 682, 268, ['moon', 'speed', 'bookmark', 'skipSilence', 'volume']);

note(20, 400, ['No pins on the player.', 'Just chapters and', 'how far you are.']);
arrow([[200, 410], [236, 400], [p.x + 30, p.y + 388]]);
note(20, 770, ['Tap: bookmarks', 'and history.', 'Long-press: save', 'a moment.']);
arrow([[200, 840], [bookmarkButton - 40, p.y + 776], [bookmarkButton - 2, p.y + 740]]);

const b = phone(740, 80, 'Bookmarks');
appBar(b.x, b.y, { title: 'Echoes of Tomorrow', subtitle: 'Daniel Hartwell', cover: true, actions: ['sort'] });
tabs(b.x + 20, b.y + 108, 320, ['Bookmarks', 'History'], 0);
const at = bookBar(b.x + 30, b.y + 182, 300, {
  progress: 0.72,
  pins: [[0.15, 'favorite'], [0.46, 'quote'], [0.65, 'revisit'], [0.83, 'note']],
  marker: true,
});
text(b.x + 30, b.y + 210, '72% of book', { size: 13 });
text(b.x + 330, b.y + 210, '4:14:24 left', { size: 13, fill: GREY, anchor: 'end' });
chip(b.x + 20, b.y + 226, 58, 'All  4', { selected: true });
['favorite', 'quote', 'revisit', 'note'].forEach((kind, i) => chip(b.x + 86 + i * 62, b.y + 226, 54, '1', { kind }));

label(b.x + 22, b.y + 284, 'Chapter 3 · Static on the Line');
row(b.x + 16, b.y + 292, 328, { kind: 'favorite', title: 'The first signal', subtitle: '12:40 · Sep 12' });
label(b.x + 22, b.y + 372, 'Chapter 8 · Mira');
row(b.x + 16, b.y + 380, 328, { kind: 'quote', title: 'Mira’s theory', subtitle: '24:31 · Thursday' });
label(b.x + 22, b.y + 460, 'Chapter 11 · Interference');
row(b.x + 16, b.y + 468, 328, { kind: 'revisit', title: 'The hidden message', subtitle: '21:48 · Yesterday' });
line(b.x + 20, b.y + 552, b.x + 92, b.y + 552, { stroke: GREY, strokeLineDash: [5, 5] });
line(b.x + 268, b.y + 552, b.x + 340, b.y + 552, { stroke: GREY, strokeLineDash: [5, 5] });
text(b.x + 180, b.y + 557, 'You are here · 14:48', { size: 13, anchor: 'middle', weight: 700 });
label(b.x + 22, b.y + 590, 'Chapter 14 · Static');
row(b.x + 16, b.y + 598, 328, { kind: 'note', title: 'Return to this later', subtitle: '36:44 · Sep 30' });
button(b.x + 160, b.y + 684, 180, 52, 'Save this moment', { filled: true, icon: 'bookmark' });

note(1130, 200, ['Pins live here, one', 'step away from the player.']);
arrow([[1130, 210], [1100, 228], [at(0.83) + 10, b.y + 160]]);
note(1130, 300, ['The kinds stay:', 'favorite, quote,', 'revisit, note.']);
arrow([[1130, 312], [1100, 318], [b.x + 334, b.y + 241]]);
note(1130, 520, ['Only what you saved.', 'The sleep timer adds', 'no “Dozed off” here,', 'that goes to History.']);
arrow([[1128, 560], [1100, 560], [b.x + 348, b.y + 552]]);
