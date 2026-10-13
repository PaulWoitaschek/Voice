// The history as stretches of listening: an accidental jump this morning, and last night with the sleep timer.
const h = phone(390, 80, 'History');
appBar(h.x, h.y, { title: 'Echoes of Tomorrow', subtitle: 'Daniel Hartwell', cover: true });
tabs(h.x + 20, h.y + 108, 320, ['Bookmarks', 'History'], 1);

function stretch(y, { from, to, when, cause, sleep = false }) {
  const r = row(h.x + 16, y, 328, {
    title: [from, { str: '→', pad: 7, style: { fill: GREY } }, to],
    subtitle: when,
    caption: cause,
  });
  if (sleep) icon.moon(h.x + 44, y + 38, 10, PIN);
  else {
    circle(h.x + 44, y + 38, 30);
    icon.play(h.x + 45, y + 38, 7);
  }
  return { ...r, from: r.title[0], to: r.title[2] };
}

label(h.x + 22, h.y + 156, 'Today', { bold: true });
const jump = stretch(h.y + 164, { from: 'Ch. 17, 01:02', to: '15:10', when: '07:58 – 08:12 · 14 min', cause: 'Started by a jump on the lock screen' });
const before = stretch(h.y + 248, { from: 'Ch. 14, 36:44', to: 'Ch. 15, 12:20', when: '07:30 – 07:58 · 28 min', cause: 'Headset' });
label(h.x + 22, h.y + 352, 'Last night', { bold: true });
const night = stretch(h.y + 360, { from: 'Ch. 13, 02:10', to: '31:05', when: '22:41 – 23:12 · 31 min', cause: 'Sleep timer ended it', sleep: true });
label(h.x + 22, h.y + 464, 'Yesterday', { bold: true });
const car = stretch(h.y + 472, { from: 'Ch. 12, 10:00', to: 'Ch. 13, 02:10', when: '17:05 – 17:50 · 45 min', cause: 'Car' });
stretch(h.y + 556, { from: 'Ch. 11, 21:48', to: 'Ch. 12, 10:00', when: '08:02 – 08:40 · 38 min', cause: 'Headset' });
stretch(h.y + 640, { from: 'Ch. 11, 02:30', to: '21:48', when: '07:31 – 07:52 · 21 min', cause: 'In the app' });

note(20, 300, ['One row per stretch', 'of listening: when, and', 'from where to where.', 'No filters, no event log.']);
arrow([[290, 350], [340, 360], [before.x - 4, before.y + 30]]);
note(20, 600, ['Tap a row to go there.']);
arrow([[250, 612], [320, 600], [car.x - 4, car.y + 34]]);

note(790, 150, ['A big jump starts', 'a new stretch.']);
arrow([[786, 172], [770, 200], [jump.from.x + jump.from.width + 2, jump.from.y + 4]]);
note(790, 280, ['Lost your place? It’s', 'where the stretch', 'before the jump ends.']);
arrow([[786, 316], [770, 330], [before.to.x + before.to.width + 4, before.to.y + 10]]);
note(790, 428, ['Still awake here…']);
arrow([[784, 420], [660, 421], [night.from.x + night.from.width - 18, night.from.y - 2]]);
note(790, 500, ['…the sleep timer', 'stopped here.']);
arrow([[784, 492], [740, 480], [night.to.x + night.to.width + 6, night.to.y + 10]]);
