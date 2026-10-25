---
name: snapshots
description: Voice's screenshot tests and preview rendering without an emulator. Every Compose `@Preview` is rendered with Robolectric, checked against one stored image per screen by `verifySnapshots` (part of `voiceUnitTest` and CI), re-recorded with `recordSnapshots`, and rendered to PNGs to look at with `snapshot`. Covers adding a preview for the case you want to see (window size, font scale, dark, right-to-left, view states), updating the stored images after a UI change, and reading failures. Use when a UI change makes `verifySnapshots` fail, to see what a composable or screen looks like, or before and after a UI change.
---

# Preview snapshots

Every library module with Compose has three tasks. Each renders every `@Preview` in the module, private ones included,
with Robolectric.

```bash
./gradlew :features:playbackScreen:snapshot          # PNGs to look at, in build/snapshots/
./gradlew :features:playbackScreen:verifySnapshots   # compares with src/test/snapshots/
./gradlew :features:playbackScreen:recordSnapshots   # rewrites src/test/snapshots/
```

Leave out the module to run it for all of them, which takes about 20 seconds.

## Screenshot tests

Each module stores one sheet per screen in `src/test/snapshots/`:

- `<Screen>.webp` holds all of that screen's previews, scaled down and labeled.
- `<Screen>.txt` lists where each preview is on the sheet, so a pull request shows in text which previews were added,
  removed or resized.

`verifySnapshots` runs as part of `voiceUnitTest`, so CI runs it too. It compares each preview with its own place on
the sheet, and names every preview that is new, removed, resized or looks different. For each changed one it writes the
stored image, the new one and the differences in red side by side to `<module>/build/snapshotDiffs/`. CI uploads that
folder as the `snapshot-diffs` artifact.

After an intended UI change:

1. Run `./gradlew :<module>:recordSnapshots`.
2. Look at the changed `<Screen>.webp`. GitHub may not show a diff for WebP images, so open the old and new one.
3. Commit the sheet together with its `.txt`.

Every committed preview needs the screen it belongs to as its group, named after the navigation destination where there
is one: `@Preview(group = "Playback")`. A preview without one fails `verifySnapshots` and `recordSnapshots`.

The sheets are kept small for plain git. They're encoded with libwebp, and phone-sized previews are stored at 0.75 px
per dp, landscape ones (at least 600 dp wide and wider than tall) at 0.5 and tablet ones (at least 600 dp both ways) at
0.375. Add landscape and tablet previews only for full screens, and make one of them dark instead of adding a dark
preview of its own.

## Looking at previews

`snapshot` writes to `<module>/build/snapshots/`:

- `_contact-sheet.png`: all previews of the module, labeled. Read this first.
- `<File>Kt.<Preview>[_<variant>].png`: one per preview at 2× density, for details.

It's skipped as up to date while the module hasn't changed.

## Showing a case

Add a preview to the module, or annotate an existing one. The annotation decides how it's rendered:

| Annotation | Rendered |
|---|---|
| `group` | which screen's sheet it's stored in |
| `widthDp`, `heightDp` | that exact size, otherwise wrapped in a 411 × 891 window |
| `fontScale = 2F` | text at 2× |
| `uiMode = Configuration.UI_MODE_NIGHT_YES` | dark |
| `locale = "iw"` (also `ar`, `fa`, `ur`) | right to left |
| `showBackground`, `backgroundColor` | that background, otherwise white, or black at night |
| `@PreviewParameter` | one image per value |

Several `@Preview` annotations on one function give one image each, and so do multipreviews like `@PreviewLightDark` and
`@PreviewFontScale`. `device`, `apiLevel` and dynamic color aren't applied.

```kotlin
@Composable
@Preview(group = "Playback", name = "landscape dark", widthDp = 891, heightDp = 411, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(group = "Playback", name = "tablet", widthDp = 1280, heightDp = 800)
private fun BookPlayWindowSizesPreview() {
  VoiceTheme {
    BookPlayView(viewState = …, …)
  }
}
```

Previews you added only to look at something don't belong in the commit, as every committed preview is a screenshot
test. Keep one if it documents a case worth keeping an eye on, and record it.

## When a preview fails to render

The tasks render every preview, then fail with the ones that crashed and their root cause. Most often a preview calls
a screen's entry point, which fetches its ViewModel through `rootGraphAs`. Preview the stateless composable underneath
instead, with a view state.

## What you see

- Previews run in inspection mode, like in Android Studio. Covers show their placeholder, and confetti always flies the
  same way.
- The clock is advanced by 1 second, so entrance animations have finished and a long title hasn't started its marquee.
  Endless animations, like the seek bar's wave, show a single frame.
- Only the preview's own window is captured. Dialogs, bottom sheets, popups and menus open in windows of their own and
  are missing. Preview their content instead.
- There are no system bars or window insets. For those, run the app on an emulator (see the emulator skill).

## How it works

- `:core:snapshot` holds `PreviewSnapshotTest`, which finds previews with ComposablePreviewScanner and renders each in
  its own activity with qualifiers from the annotation. `Sheet.kt` lays out the sheets, and `ReferenceImages.kt` stores
  and compares them.
- The `voice.compose` convention plugin (`plugins/src/main/kotlin/configurePreviewSnapshots.kt`) generates a one-line
  subclass into each module's unit test sources, excludes it from the regular test tasks, and registers the three tasks.
  They run the same test, told apart by a system property.
- The tasks pin the JVM's locale and time zone, so times and numbers are formatted the same on every machine. A pixel
  counts as different only when a color channel is off by more than 6, as Linux and macOS render a few levels apart in
  places.
- They take no Gradle properties, so the configuration cache is reused across runs.
