const p = phone(260, 80, 'Player');
const { x, y } = p;

iconButton(x + 42, y + 50, 'chevronDown');
iconButton(x + 268, y + 50, 'chapters');
iconButton(x + 318, y + 50, 'more');
image(x + 80, y + 92, 200, 200, 22);
text(x + 30, y + 334, 'Echoes of Tomorrow', { size: 23 });
text(x + 30, y + 358, 'Daniel Hartwell', { size: 15, fill: GREY });

bookBar(x + 30, y + 392, 300, { progress: 0.72 });
text(x + 30, y + 420, '72% of book', { size: 13 });
text(x + 330, y + 420, '4:14:24 left', { size: 13, fill: GREY, anchor: 'end' });

iconButton(x + 52, y + 470, 'previous', { d: 48 });
roundRect(x + 84, y + 446, 192, 48, 24);
text(x + 104, y + 465, 'Chapter 12 of 18', { size: 11, fill: GREY });
const chapter = text(x + 104, y + 484, 'The Signal', { size: 15 });
icon.chevronDown(x + 254, y + 470, 6);
iconButton(x + 308, y + 470, 'next', { d: 48 });

slider(x + 34, y + 530, 292, 0.4);
text(x + 32, y + 562, '14:48', { size: 13, fill: GREY });
text(x + 328, y + 562, '-21:12', { size: 13, fill: GREY, anchor: 'end' });

iconButton(x + 88, y + 620, 'rewind', { d: 66 });
roundRect(x + 134, y + 574, 92, 92, 30, HATCH);
icon.play(x + 182, y + 620, 15);
iconButton(x + 272, y + 620, 'forward', { d: 66 });

const [, , bookmarkButton] = toolbar(x + 46, y + 682, 268, ['moon', 'speed', 'bookmark', 'skipSilence', 'volume']);

note(20, 500, ['Tap to pick', 'a chapter.']);
arrow([[150, 506], [260, y + 434], [chapter.x + 4, y + 444]]);
note(20, 790, ['Long-press to save', 'a moment.']);
arrow([[236, 792], [bookmarkButton - 40, y + 776], [bookmarkButton - 2, y + 740]]);
