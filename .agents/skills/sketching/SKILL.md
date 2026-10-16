---
name: sketching
description: Drawing low-fi sketches of Voice screens in a hand-drawn style, from a short scene file and one render command. Use when asked to sketch, wireframe or mock up a screen, a flow or an idea, for a product spec, an issue, a discussion or a pull request, before it's built. To see how the real app looks, use the snapshots or emulator skill instead.
---

# Sketching

A sketch shows an idea before anything is built: what's on a screen, what changes, and why. It looks hand-drawn on
purpose, so the discussion stays on the idea and not on pixels.

## Draw one

1. Write a scene file named `<name>.sketch.js`, next to the Markdown that will show it. Sketches for the product
   spec go in `docs/sketches/`. Start from [examples/player.sketch.js](examples/player.sketch.js) or an existing sketch.
2. Render it from the repository root:

   ```sh
   .agents/skills/sketching/sketch docs/sketches/<name>.sketch.js
   ```

   This writes `<name>.png` next to the scene, cropped to what you drew. It takes several files at once. The first
   run installs the tools with npm. It needs Node and Chrome. With no Chrome, set `CHROME_PATH` to a Chromium binary.
3. Read the PNG. Fix overlaps, arrows that cross text, and notes that run into the phone, then render again.
4. Commit the scene with the PNG so the next person can change it, and embed the image:
   `![Sketch: what it shows](sketches/<name>.png)`.

## The scene

A scene is plain JavaScript that calls the functions in [tool/lib.js](tool/lib.js). Coordinates are pixels from the
top left. A phone is 360 × 760, so phones side by side sit about 120 apart, with room for notes left and right.

- **Screens:** `phone(x, y, title)`, `appBar`, `tabs`, `image` for a cover, `iconButton`, `button`, `chip`, `row`,
  `label`, `bookBar`, `slider`, `toolbar`.
- **Icons:** `icon.<name>(x, y)`, centered. The bookmark kinds `favorite`, `quote`, `revisit` and `note` have the shapes
  of `bookmarkStyle`.
- **Shapes and text:** `rect`, `roundRect`, `line`, `circle`, `path`, `poly`, `curve`, `text`. `text` returns its box,
  and `run` returns a box per part of a line, to point arrows at.
- **Notes:** `note(x, y, lines)` writes in orange handwriting and `arrow(points)` points from a note to what it
  explains. With three points the arrow curves.
- **Styles:** `INK`, `GREY` and `LIGHT` for strokes and text. `HATCH` fills a primary button or a selected chip, and
  `SOLID` fills something dark.

## What makes a good sketch

- **One idea per sketch.** Show the screen that changes. If the idea is about moving something, show both screens.
- **Notes on what's new or different.** Don't label everything. A note says what it does for the listener, not which
  component it is.
- **Grey screens, orange notes.** That's the only color. A sketch never suggests a color scheme.
- **Real words.** Use the strings from `core/strings/src/main/res/values/strings.xml` and the demo book from
  `KioskModeDemoData` ("Echoes of Tomorrow" by Daniel Hartwell), in English.
- **Keep notes outside the phone**, and route arrows through empty space.
