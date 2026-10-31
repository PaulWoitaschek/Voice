---
name: feature-issue
description: Opening a GitHub issue for a feature the maintainer committed to. Use when asked to open, create or write a feature issue, or to turn an idea discussion into an issue.
---

# Feature issue

Issues hold the bugs and features that will get done. Ideas stay in Discussions until the maintainer commits to one,
so only open a feature issue when asked to.

- **Title:** what changes for listeners, like a pull request title.
- **Body:** the problem in one to three sentences, then what will change. Link the idea discussion if there is one.
- **Label:** `feature`.

```sh
gh issue create --label feature --title "<title>" --body "<body>"
```
