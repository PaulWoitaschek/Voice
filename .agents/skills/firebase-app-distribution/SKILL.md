---
name: firebase-app-distribution
description: Uploading a Play release build of Voice to Firebase App Distribution from the local machine. Use when asked to distribute, upload, or send a tester or beta build to Firebase or to testers.
---

# Firebase App Distribution

Run `./scripts/firebase_app_distribution.sh`. It builds the Play release APK of the latest commit and uploads it with
`<short hash>: <commit subject>` as release notes.

- Testers in `beta-testers` are notified by default. Pass `--groups <aliases>` (comma-separated) to notify other
  groups instead, but only when the user names them.
- Uploading notifies real testers, so confirm with the user before running it, including the groups.
- The script refuses to run with uncommitted changes. Don't commit on the user's behalf to get around this. Ask them.

## Errors

- Authentication or permission errors: the user must run `npx firebase-tools@<version> login` themselves, since it's
  interactive. Take the version from `firebase_tools_version` in the script. A set `GOOGLE_APPLICATION_CREDENTIALS`
  takes precedence over that login.
- Missing `signing/signing.properties` or `app/src/play/google-services.json`: these are secrets that aren't in the
  repository. Ask the user for them. Don't work around them, since an unsigned or differently signed build won't
  install over the Play Store version.
