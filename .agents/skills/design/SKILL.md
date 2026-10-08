---
name: design
description: Voice's product and design principles. Covers which features belong in the app, Material 3 Expressive, color, shape, motion, delight, and accessibility. Use when designing, building, or reviewing UI (screens, sheets, components, widgets, empty states, motion), or when deciding whether and how to add a feature or setting.
---

# Designing for Voice

Voice is a calm, playful audiobook player for people who love stories. Every screen should be easy to use and still
feel good to use.

## Clean, not minimal

Voice can do a lot for listeners, as long as each screen stays calm. Keep the number of things on screen and the
decisions a listener has to make small. The feature set doesn't have to be.

- A feature has to make listening better for many listeners, not one power user. Every merged feature is maintained
  for years.
- Prefer good behavior to a setting. Pick a sensible default and make it smart: auto rewind, the jump-back pill after
  a big seek. Add a setting only when listeners really want different things.
- Change settings in place, with steppers, preset chips, or small previews, not in a dialog (see the Listening
  section in settings).
- Layer it. The main screen shows what you need while listening. Details are one step away, in a sheet, a tab, or a
  long-press. A long-press on the bookmark button saves right away, and the editor sheet holds the details.
- One primary action per screen or sheet. Secondary actions are quieter. Destructive ones use the error colors and
  need a deliberate step, like ticking "Delete my files" first.
- Never interrupt listening. Prompts such as rating or support wait for a calm moment, ask once, and accept no.
- No account, no ads, nothing behind a paywall, and the data stays on the device.

## Material 3 Expressive

Lean expressive: bold shapes, rich color, springy motion.

- `VoiceTheme` wraps `MaterialExpressiveTheme`. Use the expressive components: `LargeFlexibleTopAppBar`,
  `AppBarWithSearch`, `ToggleButton` groups, `LoadingIndicator`, `LinearWavyProgressIndicator`,
  `HorizontalFloatingToolbar`, `FilterChip`. Titles use the `*Emphasized` type styles.
- Use sheets for new flows, not dialogs: a `ModalBottomSheet` with a header that shows what it's about (cover, title).
  Rename, delete, and book actions are examples.
- Group related content in large rounded containers, like the 32dp islands in settings. For connected rows, use
  `segmentedShape`.

## Color

- Use only `MaterialTheme.colorScheme` roles, in container/on-container pairs or with `contentColorFor`. Don't
  hardcode colors. Everything has to work with every palette in `ThemeColorScheme`, with Dynamic color, and in light
  and dark.
- Book-centric surfaces take their colors from the cover: the player and the book actions sheet through
  `CoverTheme(cover)`, and the Glance widgets through `coverColorScheme`. App-wide screens like settings don't.
- Sleep-related UI uses `NightTheme`. Color changes blend over and never jump.
- Check dark mode on its own. With the 2025 spec, `tertiaryContainer` is a light tone there, so settings uses a deeper
  tint instead.

## Shape and icons

- Shapes carry meaning. Use `ShapedIcon` with `MaterialShapes` (cookies, clover, sunny, soft burst, flower) to make
  icon stickers. Give each concept one shape and color and keep it the same everywhere (folder modes,
  `bookmarkStyle`).
- Never convey meaning by color alone. Pair the color with a shape and an icon.
- A selected item can change its shape (`MorphShape`).
- Icons come only from `VoiceIcons`. To add one, list it in `scripts/generate_material_symbols.sh` and regenerate.
  Mirror directional icons, like chevrons and back arrows, in RTL.

## Motion and delight

- Use `MaterialTheme.motionScheme` specs or springs. Bouncy springs are for small pops like check badges.
- Covers and the search pill move between screens as shared elements (`LocalSharedTransitionScope`). Screens enter
  with a stagger (`rememberEntranceState` / `Modifier.entrance`).
- Continuous motion, like the aurora and the waves, runs on `rememberAnimationClock`. It only ticks while something is
  happening and eases to a halt.
- Use delight in small doses. Give a screen one hero at most: the aurora, an illustration, or a cover. The rest is
  plain content.
- Illustrations react to state, like the skip illustration or the starry night card when the sleep timer is on.
- Celebrate only real moments, like a finished book, a milestone, or a saved bookmark, with confetti, a pop, or a
  haptic (`HapticFeedbackType.Confirm`, `SegmentTick`).
- Delight never blocks a task or adds a tap.
- With animations off (`MotionDurationScale` is 0), there's no confetti, the aurora stands still, and everything
  still works.

## Accessibility

- Hide decorative shapes and illustrations from accessibility services (`contentDescription = null`).
- Make the whole row the control, with `toggleable(role = Role.Switch)`. Mark section titles with
  `semantics { heading() }`. Presets are radio buttons in a selectable group.
- With large fonts, the layout reflows. Heroes give up space first. Tile grids fall back to lists at
  `fontScale >= 1.5F`, like `HelpSection`.
- Keep text readable over the aurora and cover colors in both themes.

## Building blocks

Check `:core:ui` before building something new. It has `ShapedIcon`, `MorphShape`, `segmentedShape`,
`AuroraBackground`, `rememberAnimationClock`, `Modifier.entrance`, `ConfettiState`, `BeatingHeart`, `CoverTheme`,
`NightTheme`, `PlayButton`, `BookBar`, `NowPlayingBars`, `bookmarkStyle`, and `OnboardingScaffold`. Pieces that only
one feature uses stay in that feature (see the architecture skill). For wording, follow the tone in the
translate-strings skill.

## Checking a design

On an emulator, check light and dark, a non-default palette, Dynamic color, large fonts (1.5× and 2×), animations off,
RTL, and a short landscape screen. For cover-colored screens, try a very bright cover and a very dark one.
