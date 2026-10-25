---
name: snapshots
description: Rendering Voice's Compose previews to PNG images without an emulator, with the per-module `snapshot` Gradle task. Every preview also comes out dark, and screens also in landscape and on a tablet. Covers adding a preview for other cases (split screen, font scale, right-to-left, view states) and reading the results. Use to see what a composable or screen looks like, before and after a UI change, or at sizes and font scales that are hard to set up on a device.
---

# Preview snapshots

```bash
./gradlew :features:playbackScreen:snapshot   # one module, a few seconds
./gradlew snapshot                             # every module, about 30 seconds
```

Every library module with Compose has a `snapshot` task. It renders each `@Preview` in the module, private ones
included, with Robolectric, and writes one PNG per preview and an overview to `<module>/build/snapshots/`:

- `_contact-sheet.png`: all previews of the module, labeled, each next to its other versions. Read this first.
- `<File>Kt.<Preview>[_<variant>].png`: one per preview at 2× density, for details.

Besides each preview as annotated, the task renders other versions of it:

- `…_dark`: dark, unless the preview sets `uiMode` itself.
- `…_landscape` (891 × 411 dp) and `…_tablet` (1280 × 800 dp): only for screens, meaning previews without a size of
  their own that fill the whole window. Cards, rows and bars don't get them.

The task only runs when asked for. It isn't part of `testDebugUnitTest`, `voiceUnitTest` or CI, and it's skipped as up
to date while the module hasn't changed. It's for looking, not for comparing against stored images.

## Showing a case

Dark, landscape and tablet come without a preview of their own. For anything else, add a preview to the module, or
annotate an existing one. The annotation decides how it's rendered:

| Annotation | Rendered |
|---|---|
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
@Preview(widthDp = 384, heightDp = 300, name = "split screen")
@Preview(fontScale = 2F, name = "large text")
private fun PlayerCasesPreview() {
  VoiceTheme {
    BookPlayView(viewState = …, …)
  }
}
```

Previews you added only to look at something don't belong in the commit. Keep one if it documents a case worth
keeping an eye on.

## When a preview fails

The task renders every preview, then fails with the ones that crashed and their root cause. Most often a preview calls
a screen's entry point, which fetches its ViewModel through `rootGraphAs`. Preview the stateless composable underneath
instead, with a view state.

## What you see

- The clock is advanced by 1 second, so entrance animations have finished and a long title hasn't started its marquee.
  Endless animations, like the seek bar's wave, show a single frame.
- The existing previews pass no cover, so covers show their placeholder.
- A preview that doesn't wrap itself in `VoiceTheme` ignores dark mode, so its dark version looks broken. Wrap it in
  `VoiceTheme`, and in a `Surface` when it has no background of its own.
- Only the preview's own window is captured. Dialogs, bottom sheets, popups and menus open in windows of their own and
  are missing. Preview their content instead.
- There are no system bars or window insets. For those, run the app on an emulator (see the emulator skill).

## How it works

- `:core:snapshot` holds `PreviewSnapshotTest`, which finds previews with ComposablePreviewScanner and renders each in
  its own activity with qualifiers from the annotation. The other versions render the same preview again, with the
  annotation's mode or size swapped.
- The `voice.compose` convention plugin (`plugins/src/main/kotlin/configurePreviewSnapshots.kt`) generates a one-line
  subclass into each module's unit test sources, excludes it from the regular test tasks, and registers `snapshot`.
- It takes no Gradle properties, so the configuration cache is reused across runs.
