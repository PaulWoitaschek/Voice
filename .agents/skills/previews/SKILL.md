---
name: previews
description: Rendering Voice's Compose previews to PNG images without an emulator, with `android preview` from the Android CLI. Covers finding and rendering previews, adding one for a case (dark, landscape, tablet, split screen, font scale, right-to-left, view states) and reading the results. Use to see what a composable or screen looks like, before and after a UI change, or at sizes and font scales that are hard to set up on a device.
---

# Rendering previews

`android preview` from the [Android CLI](https://developer.android.com/tools/agents/android-cli) builds a module and
renders its `@Preview`s to PNGs on this machine, private ones included. If `which android` finds nothing, ask the user
to install it.

```bash
android preview list :app@freeDebug --all        # every preview in the project
android preview list :features:playbackScreen    # one module's previews
android preview render :features:playbackScreen \
  --fqn=voice.features.playbackScreen.view.BookPlayViewKt.BookPlayPreview
```

- `render` needs at least one `--fqn`. Repeat it to render several previews in one build. Through `:app@freeDebug` it
  renders previews from every module. One module takes a few seconds, every preview about 15.
- `:app` needs its variant, `@freeDebug`. Library modules don't.
- It prints the path of each PNG, in a temporary directory unless you pass `-o <dir>`. They're named
  `<File>Kt/<Preview>_<n>.png`, one per annotation and `@PreviewParameter` value, numbered in the order of the
  annotations.
- Images come at the device's density, so a phone screen is 1080 × 2400 px.
- `--component-tree` also writes a JSON file per image with each composable's bounds, semantics and source location.

## Showing a case

A preview renders only as it's annotated, and most are light phone previews. For anything else, add a preview, or
annotate an existing one:

| Annotation | Rendered |
|---|---|
| `@PreviewLightDark` | light and dark |
| `uiMode = Configuration.UI_MODE_NIGHT_YES` | dark |
| `widthDp`, `heightDp` | that exact size, otherwise the size of its content, at most a phone screen |
| `device = "spec:width=891dp,height=411dp"` | a short landscape phone |
| `device = Devices.TABLET` | a 1280 × 800 dp tablet |
| `fontScale = 2F` | text at 2× |
| `locale = "ar"` | that locale's strings, but still left to right |
| `showBackground`, `backgroundColor` | that background |
| `@PreviewParameter` | one image per value |

Several `@Preview` annotations on one function give one image each, and so do multipreviews like `@PreviewLightDark`
and `@PreviewScreenSizes`. For right to left, wrap the content in
`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.

```kotlin
@Composable
@PreviewLightDark
@Preview(widthDp = 384, heightDp = 300, name = "split screen")
@Preview(device = "spec:width=891dp,height=411dp", name = "landscape")
@Preview(device = Devices.TABLET, name = "tablet")
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

The other previews still render. The command then exits with 1 and prints each failure's stack trace, with the root
cause under `Caused by`. Most often a preview calls a screen's entry point, which fetches its ViewModel through
`rootGraphAs`. Preview the stateless composable underneath instead, with a view state.

## What you see

- The existing previews pass no cover, so covers show their placeholder.
- A preview that doesn't wrap itself in `VoiceTheme` ignores dark mode. Wrap it in `VoiceTheme`, and in a `Surface`
  when it has no background of its own.
- Only the preview's own window is captured. Dialogs, bottom sheets, popups and menus open in windows of their own and
  are missing. Preview their content instead.
- There are no system bars or window insets, even with `showSystemUi`. For those, run the app on an emulator (see the
  emulator skill).
