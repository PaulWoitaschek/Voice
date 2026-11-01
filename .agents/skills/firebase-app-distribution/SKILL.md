---
name: firebase-app-distribution
description: Uploading a Play release build of Voice to Firebase App Distribution, locally or with the GitHub workflow. Use when asked to distribute, upload, or send a tester or beta build to Firebase or to testers.
---

# Firebase App Distribution

Notify `beta-testers`, unless the user names other groups.

## Locally

Use this when `signing/signing.properties` in the main checkout and `app/src/play/google-services.json` exist, and
`npx firebase-tools@<version> login:list` shows an account or `GOOGLE_APPLICATION_CREDENTIALS` is set. The version is
`firebase_tools_version` in the script.

```sh
./scripts/firebase_app_distribution.sh                 # --groups <aliases> for other groups
```

It builds the working tree. With uncommitted changes, the commit in the version name and the notes gets `-dirty`.

When it's done, post the tester link, the URL after `Share this release with testers who have access`. Testers open it
on their phone to install the build.

## GitHub workflow

Otherwise, ask first. Say that it builds the pushed branch, so uncommitted or unpushed changes need to be committed and
pushed first.

```sh
gh workflow run firebase_app_distribution.yml --ref <branch> -f groups=beta-testers
```

## Errors

- Firebase login: the user runs `npx firebase-tools@<version> login`, it's interactive.
- `gh`: the version counts draft releases. The user runs `gh auth login`.
- Missing signing or `google-services.json`: these are secrets, ask the user. Don't work around them, a differently
  signed build won't install over the Play Store version.
