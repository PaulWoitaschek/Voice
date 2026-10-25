'use strict';

// Made-up books. They cover the cases of the library spec: series from tags, series that aren't tagged,
// a radio play whose episodes share an album, one author spelled three ways, hidden books, and books
// from a folder, a kids' folder and an Audiobookshelf server.

const SOURCES = [
  { id: 'audiobooks', name: 'Audiobooks', server: false },
  { id: 'kids', name: 'Kids', server: false },
  { id: 'abs', name: 'audiobooks.example.com', server: true },
];

const pad = (n, length) => String(n).padStart(length, '0');

const FOLDERS = {
  sternenpiraten: {
    title: 'Die Sternenpiraten',
    author: 'Hanna Wiese',
    hue: 280,
    motif: 'stars',
    splitMotif: 'episode',
    files: [
      'Der Start', 'Kurs auf Orion', 'Meteoritenschauer', 'Der blinde Passagier', 'Funkstille', 'Die Eiswelt',
      'Verrat an Bord', 'Der Kometenschweif', 'Notlandung', 'Die Sternenkarte', 'Das Wurmloch', 'Heimkehr',
    ].map((name, i) => ({ name: `Folge ${pad(i + 1, 2)}`, title: `Folge ${pad(i + 1, 2)}: ${name}`, duration: 48 * 60 })),
  },
};

const NACHTFALKEN = [
  [1, 'Das Geheimnis der Mühle'], [2, 'Der verschwundene Pokal'], [3, 'Spuk im Leuchtturm'],
  [4, 'Die Nacht der Eulen'], [5, 'Der falsche Kapitän'], [6, 'Das Rätsel im Moor'], [7, 'Die gestohlene Glocke'],
  [8, 'Gefahr am Steinbruch'], [9, 'Der Schatz im Kirchturm'], [10, 'Die Stimme aus dem Brunnen'],
  [11, 'Das Phantom vom Jahrmarkt'], [12, 'Die rote Laterne'], [13, 'Der Fluch der Wassermühle'],
  [14, 'Das Geisterschiff'], [15, 'Die Spur im Schnee'], [16, 'Das Haus am Waldrand'], [62, 'Der Schattenreiter'],
  [63, 'Monster-Wolken'], [64, 'Die letzte Fähre'], [163, 'Das Echo im Tal'],
];

const CALLOWAY = [
  ['Winter Harbor', { added: 40 }],
  ['The Glass Orchard', { finished: true, last: 120, added: 300 }],
  ['Under the Linden Tree', { added: 300 }],
  ['A House of Small Rooms', { added: 300, light: true }],
  ["The Ferryman's Wife", { finished: true, last: 250, added: null }],
  ['Paper Boats', { added: null, light: true }],
  ['The Summer of Quiet Things', { added: 41, light: true }],
  ['What the Tide Brings', { added: 41 }],
  ['The Last Orchard Road', { added: 300 }],
  ['Letters to Elsie', { added: 300, hidden: true, light: true }],
  ['The Weight of Feathers', { added: 300 }],
  ['A Season of Small Mercies', { added: 41, light: true }],
  ['The Apple Thief', { added: 300, hidden: true }],
  ['Evening at Larkspur', { added: 300 }],
];

// A folder picked in the preview of "Voice finds the books", with what Voice guesses for each folder in it.
const PREVIEW = {
  source: { id: 'new', name: 'New audiobooks', server: false },
  items: [
    { key: 'hollow-crown', title: 'The Hollow Crown', author: null, guess: 'book', single: true, hue: 350, motif: 'crown', files: [{ name: 'The Hollow Crown.m4b', title: 'The Hollow Crown', duration: 11 * 3600 }] },
    { key: 'quiet-rooms', title: 'Quiet Rooms', author: 'Weller', guess: 'book', hue: 120, motif: 'house', files: Array.from({ length: 12 }, (_, i) => ({ name: pad(i + 1, 2), title: pad(i + 1, 2), duration: 35 * 60 })) },
    { key: 'long-field', title: 'The Long Field', author: 'Weller', guess: 'book', hue: 90, motif: 'mountains', files: Array.from({ length: 9 }, (_, i) => ({ name: pad(i + 1, 2), title: pad(i + 1, 2), duration: 53 * 60 })) },
    { key: 'ember-saga', title: 'The Ember Saga', author: 'Castell', guess: 'series', hue: 12, motif: 'sun', files: [{ name: '1 The Ember Throne', title: 'The Ember Throne', duration: 13 * 3600 }, { name: '2 The Ember Crown', title: 'The Ember Crown', duration: 14 * 3600 }] },
    { key: 'waldgeister', title: 'Waldgeister', author: null, guess: 'book', hue: 140, motif: 'leaf', files: Array.from({ length: 40 }, (_, i) => ({ name: `Folge ${pad(i + 1, 3)}`, title: `Folge ${pad(i + 1, 3)}`, duration: 45 * 60 })) },
    { key: 'drowned-city', title: 'The Drowned City', author: null, guess: 'series', hue: 200, motif: 'waves', files: [{ name: 'Part 1 - The Beginning', title: 'Part 1 - The Beginning', duration: 9 * 3600 + 760 }, { name: 'Part 2 - The Escape', title: 'Part 2 - The Escape', duration: 10 * 3600 + 312 }] },
  ],
};

function seedBooks() {
  const B = (id, title, author, duration, options = {}) => ({ id, title, author, duration, ...options });
  const signal = { series: 'The Signal Saga', narrator: 'Kate Rowan', genre: 'Science Fiction' };
  const maple = { series: 'Maple Street Mysteries', narrator: 'Mark Ellis', genre: 'Mystery' };
  const harbor = { series: 'Harbor Lights', narrator: 'Mark Ellison', genre: 'Romance', added: null };
  const clockwork = { series: 'The Clockwork Archive', genre: 'Fantasy', added: 3 };
  const tidewater = { series: 'Tidewater Chronicles', genre: 'Fantasy, Young Adult', added: 220, source: 'abs', motif: 'waves' };
  return [
    B('signal-1', 'Static on the Line', 'Daniel Hartwell', '9:12:40', { ...signal, part: 1, finished: true, last: 40, added: 400, hue: 205, motif: 'signal' }),
    B('signal-2', 'Echoes of Tomorrow', 'Daniel Hartwell', '14:28:15', { ...signal, part: 2, left: '4:03:15', last: 0.01, added: 400, hue: 222, motif: 'planet' }),
    B('signal-3', 'Interference', 'daniel hartwell', '11:02:18', { ...signal, part: 3, added: 400, hue: 188, motif: 'signal' }),
    B('signal-4', "Mira's Theory", 'Daniel Hartwell', '9:45:51', { ...signal, part: 4, added: 120, hue: 240, motif: 'orbit' }),
    B('ashford-1', 'A Winter in Ashford', 'Daniel Hartwell', '8:20:11', { series: 'Stars over Ashford', part: 1, finished: true, last: 90, added: 300, hue: 250, motif: 'stars', light: true, narrator: 'Kate Rowan' }),
    B('ashford-2', 'The Ashford Lights', 'Daniel Hartwell', '9:01:45', { series: 'Stars over Ashford', part: 2, added: 300, hue: 268, motif: 'moon', narrator: 'Kate Rowan' }),
    B('last-broadcast', 'The Last Broadcast', 'Daniel Hartwell', '9:12:40', { hidden: true, added: 200, hue: 2, motif: 'signal' }),
    B('night-shift', 'Night Shift Radio', 'Hartwell, Daniel', '6:40:02', { added: 25, hue: 330, motif: 'signal', light: true }),

    B('maple-1', 'The Bookshop on Maple Street', 'Lena Morgan', '7:12:30', { ...maple, part: 1, left: '4:19:55', last: 1, added: 200, hue: 18, motif: 'house' }),
    B('maple-2', 'Murder at the Maple Street Fair', 'Lena Morgan', '7:48:00', { ...maple, part: 2, added: 200, hue: 34, motif: 'house', light: true }),
    B('maple-3', 'The Ghost of Maple Street', 'Lena Morgan', '8:05:12', { ...maple, part: 3, added: 200, hue: 8, motif: 'moon' }),
    B('maple-4', 'A Maple Street Christmas', 'Lena Morgan', '6:55:40', { ...maple, part: 4, added: 200, hue: 355, motif: 'stars', light: true }),
    B('maple-5', 'Last Call on Maple Street', 'Lena Morgan', '7:30:21', { ...maple, part: 5, added: 120, hue: 26, motif: 'house' }),
    B('harbor-1', 'Harbor Lights', 'Lena Morgan', '8:10:00', { ...harbor, part: 1, finished: true, last: 60, hue: 196, motif: 'lighthouse' }),
    B('harbor-2', "The Lighthouse Keeper's Daughter", 'Lena Morgan', '9:02:33', { ...harbor, part: 2, finished: true, last: 45, hue: 186, motif: 'lighthouse', light: true }),
    B('harbor-3', 'Storm Season', 'Lena Morgan', '8:44:10', { ...harbor, part: 3, finished: true, last: 20, hue: 212, motif: 'rain' }),
    B('harbor-4', 'Calm Waters', 'Lena Morgan', '7:58:45', { ...harbor, part: 4, hue: 178, motif: 'waves', light: true }),

    ...[1, 2, 3, 4, 5, 6].map(i => B(`lantern-${i}`, `The Lantern Keepers ${i}`, 'Mara Quill', `${5 + (i % 3)}:${pad(12 + i * 7, 2)}:${pad(i * 9, 2)}`, {
      file: `The Lantern Keepers ${pad(i, 2)}`, added: 45, hue: 38 + i * 5, motif: 'lantern', number: i, narrator: 'Ada Brooks', genre: 'Fantasy',
    })),
    B('quiet-orbit', 'The Quiet Orbit', 'Mara Quill', '7:20:00', { finished: true, last: 60, added: 300, hue: 230, motif: 'orbit', light: true }),

    B('map-small-hours', 'A Map of Small Hours', 'Ines Carver', '8:41:12', { added: 0, hue: 160, motif: 'mountains', light: true }),
    B('clockwork-1', 'The Clockwork Archive', 'Theo Brandt', '10:12:00', { ...clockwork, part: 1, hue: 30, motif: 'gear' }),
    B('clockwork-2', 'The Brass Library', 'Theo Brandt', '11:40:24', { ...clockwork, part: 2, hue: 40, motif: 'gear', light: true }),
    B('clockwork-3', 'The Silent Gears', 'Theo Brandt', '9:58:02', { ...clockwork, part: 3, hue: 22, motif: 'gear' }),
    B('river-remembers', 'The River Remembers', 'Amara Okafor', '9:30:00', { finished: true, last: 150, added: 380, hue: 172, motif: 'waves' }),
    B('cold-light', 'Cold Light', 'Jonas Lindqvist', '8:02:10', { added: 70, hue: 200, motif: 'mountains', narrator: 'Mark Ellison', genre: 'Thriller' }),
    B('orange-grove', 'The Orange Grove', 'Sofia Marín', '7:44:30', { finished: true, last: 200, added: null, hue: 30, motif: 'sun', light: true }),
    B('rain', 'Rain on a Tin Roof', null, '1:00:00', { added: 10, hue: 214, motif: 'rain', genre: 'Sleep' }),
    B('moonlit-gardens', 'Moonlit Gardens', 'Elena Petrova', '6:15:00', { progress: 0.15, last: 3, added: 8, hue: 140, motif: 'moon' }),

    ...NACHTFALKEN.map(([n, name]) => B(`nachtfalken-${n}`, `Folge ${pad(n, 3)}: ${name}`, 'Hanna Wiese', `0:${40 + (n % 17)}:${pad((n * 13) % 60, 2)}`, {
      series: 'Die Nachtfalken', part: n, number: n, source: 'kids', narrator: 'Jens Albrecht', genre: 'Hörspiel', hue: 248 + (n % 5) * 7, motif: 'episode',
      added: 60, finished: n <= 12, last: n <= 12 ? 30 - n : n === 13 ? 2 : null, progress: n === 13 ? 0.3 : 0,
    })),
    B('little-lighthouse', 'The Little Lighthouse', 'Greta Holm', '0:48:00', { finished: true, last: 30, added: 100, source: 'kids', hue: 192, motif: 'lighthouse', light: true }),
    B('paper-moon', 'Pip and the Paper Moon', 'Greta Holm', '1:05:00', { added: 100, source: 'kids', hue: 48, motif: 'moon', light: true }),
    B('sternenpiraten', 'Die Sternenpiraten', 'Hanna Wiese', '9:36:00', { position: '2:44:00', last: 5, added: 150, source: 'kids', hue: 280, motif: 'stars', folderKey: 'sternenpiraten' }),

    B('horizon', 'Beyond the Horizon', 'Alexander Reeves', '10:37:13', { left: '7:26:05', last: 4, added: 18, source: 'abs', hue: 28, motif: 'compass', narrator: 'Mark Ellis', genre: 'Adventure, Travel' }),
    B('cartographer', "The Cartographer's Daughter", 'Alexander Reeves', '12:03:40', { added: 18, source: 'abs', hue: 45, motif: 'compass', light: true, genre: 'Adventure' }),
    B('salt-silence', 'Salt and Silence', 'Nora Whitfield', '11:20:05', { added: 0, source: 'abs', hue: 184, motif: 'waves', light: true }),
    ...CALLOWAY.map(([title, options], i) => B(`calloway-${i + 1}`, title, 'Ruth Calloway', `${7 + (i % 5)}:${pad((i * 17) % 60, 2)}:${pad((i * 31) % 60, 2)}`, {
      source: 'abs', hue: 15 + i * 9, motif: ['leaf', 'house', 'sun', 'waves', 'lines'][i % 5], narrator: i % 2 ? 'Mark Ellison' : 'Mark Ellis', genre: 'Fiction', ...options,
    })),
    B('tidewater-1', 'Tidewater', 'Henrik Vale', '13:05:00', { ...tidewater, part: 1, hue: 168 }),
    B('tidewater-2', 'The Drowned Bells', 'Henrik Vale', '12:41:18', { ...tidewater, part: 2, hue: 176 }),
    B('tidewater-3', 'Saltmarsh King', 'Henrik Vale', '14:10:52', { ...tidewater, part: 3, hue: 184 }),
  ];
}
