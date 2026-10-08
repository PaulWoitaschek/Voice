---
name: translate-strings
description: Voice's tone of voice and translating its UI strings into all locales. Use when adding, changing, or removing strings in core/strings/src/main/res/values/strings.xml, when translating or backfilling a locale, or when reviewing translations.
---

# Translating strings

English source: `core/strings/src/main/res/values/strings.xml`. Translations: `values-<locale>/strings.xml`, one folder
per locale in [locales.md](locales.md).
Since Weblate was removed, agents write the translations and no translator community catches mistakes. Every string
has to read as if a native speaker wrote it for this app.

## Voice's tone

Voice is a calm, friendly audiobook player for people who love stories. It talks like a helpful friend, not like a
system or an ad. This applies to new English strings as well as translations.

- Plain and warm: short sentences, everyday words. "Lost your place?", "Nothing here yet".
- Playful where the English is: "Dozed off", "Hello, night owl", "%d hours of stories!". Find the local equivalent, not
  a literal translation. If nothing natural exists, go plain rather than forced.
- Never salesy or pushy. The rating and donation strings ask once, gently.
- Talk about stories and listening, not "content" or "media". Technical words only where the thing is technical
  (folders, files, permissions).
- Meaning over words. Idioms become local idioms: "so you don't lose the thread" is "damit Du den Faden nicht
  verlierst" in German and "話の流れを見失いません" in Japanese.
- For system concepts (notification, widget, battery optimization, permission), use the words Android uses in that
  language.

## Rules

- Translate every new or changed string into every locale in the same PR. CI runs
  `scripts/check_locale_config.main.kts`, which requires `app/src/main/res/xml/locales_config.xml` to list exactly the
  locales at 100% coverage, so a single missing translation in a complete locale fails the build.
- A backfill that brings a locale to 100% adds it to `locales_config.xml` with its BCP 47 tag (folder `in` is `id`,
  `iw` is `he`, `pt-rBR` is `pt-BR`).
- When the meaning of an English string changes, rewrite every translation. A casing or punctuation fix in English
  needs no translation change. A removed string is removed from every locale.
- Follow [locales.md](locales.md) for each locale's form of address, quotation marks, and plural quantities.
- Within a locale, consistency beats a nicer word: reuse the terms its file already uses (bookmark, chapter, sleep
  timer, library, skip silence, narrator). Where the file contradicts `locales.md`, follow `locales.md`.
- Keep labels about as short as the English. Buttons, tabs, chips, and stat labels have little room; find a shorter
  wording rather than an unusual abbreviation.
- Use the language's own capitalization (German nouns, sentence case elsewhere), not English Title Case.
- Don't translate Voice, Google Play, GitHub, Ko-fi, or Bluetooth. Inflect Voice where the grammar needs it
  (`Voice-szal`, `Voice\'u`), but never translate it as the word "voice".
- Keep emoji.

## Format

- Keep placeholders (`%s`, `%d`, `%1$s`). Reorder positional arguments when the grammar needs it.
- Every plural item keeps its `%d`, including `one`: in ru and uk, `one` also covers 21, 31, …, and in fr and pt-rBR it
  also covers 0.
- Escape `'` as `\'` and `"` as `\"`, and write `&` as `&amp;`. Typographic quotes (“ „ « 「) need no escaping.
- Use `…`, never `...`.
- Insert each entry at its position in `values/strings.xml`, after the key that precedes it there. Locale files have
  no comments and no `tools:` attributes. Indent entries with 4 spaces and plural items with 8.

## Workflow

1. From the repository root, list what's missing:
   `.agents/skills/translate-strings/scripts/missing_translations.main.kts [locale ...]`. It prints each untranslated
   string with its English text, its translator note, and the locales that lack it, then any placeholder mismatches
   and obsolete keys.
2. Understand each string before translating it. Read the `<!-- -->` note above the English string. For short or
   ambiguous ones ("Set", "Keep", "Revisit"), look at the call site: `history.suggestion.keep` is
   `R.string.history_suggestion_keep`. When a new English string has no note, add one; every locale benefits.
3. Translate. Read the target locale's `strings.xml` first to pick up its terms and voice. For more than a few
   strings, split the work across four parallel subagents, one per group in `locales.md`. Give each the keys with their
   English text and notes, and tell it to follow this skill.
4. Verify:
   - `.agents/skills/translate-strings/scripts/missing_translations.main.kts` lists none of your keys and no problems.
   - `./scripts/check_locale_config.main.kts`
   - `./gradlew :app:lintFreeDebug`, which catches missing plural quantities and broken format strings.
