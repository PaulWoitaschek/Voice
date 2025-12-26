# Feature Spec: folder_picker_in_settings

## Overview
Add a feature flag that relocates the "Audiobook folders" entrypoint from the Book Overview top bar into Settings. When enabled, the Settings entry appears at the very top of the list and the Book Overview folder icon is hidden. Navigation should match the current icon behavior (opens the folder picker).

## Goals
- Provide a Settings-based entrypoint to manage folders when the flag is enabled.
- Ensure only one entrypoint is visible at a time (either Settings or Book Overview).
- Preserve existing navigation behavior (open the folder picker).

## Non-goals
- Redesign the folder picker UI or flows.
- Change onboarding or scanning logic.
- Introduce new permissions or storage behaviors.

## Feature Flag
- Key: `folder_picker_in_settings`
- Default: `false`
- Scope: UI only
- Behavior:
  - `true`: show Settings entry; hide Book Overview folder icon.
  - `false`: keep current UI (folder icon in Book Overview; no Settings entry).

## UI/UX
- Settings list: add "Audiobook folders" entry at the very top (before dark theme, analytics, etc.).
  - Label: reuse existing `audiobook_folders_title` string.
  - Icon: same as Book Overview folder icon (`Icons.Outlined.Book`).
  - Action: navigate to folder picker.
- Book Overview top bar: hide the folder icon when the flag is enabled.

## Behavioral Details
- Navigation target: `Destination.FolderPicker`.
- Book Overview:
  - Flag on: no folder icon shown; no add-book hint anchored to that icon.
  - Flag off: unchanged.
- Settings:
  - Flag on: entry visible at top; navigates to folder picker.
  - Flag off: entry hidden.

## Edge Cases
- Search active in Book Overview: current behavior hides trailing icons; with flag enabled there is no folder icon anyway.
- New users with no books: Settings entry still works; folder picker handles empty state.

## Acceptance Criteria
- With flag off:
  - Book Overview shows folder icon; Settings does not show folder entry.
- With flag on:
  - Book Overview folder icon is hidden.
  - Settings shows "Audiobook folders" at top.
  - Tapping Settings entry opens folder picker.
