# Sleep timer

Listeners fall asleep to a book and find their place the next night.

## Solved

- **The book keeps playing after I fall asleep.** A timer from the moon button, in minutes or to the end of the chapter.
  It only counts listening time.
- **I forget to set the timer.** The automatic sleep timer starts one when playback begins at night.
- **The stop wakes me up.** The last 10 seconds fade out, and Voice rewinds a little.
- **The timer stops while I'm still awake.** Shaking the phone within 30 seconds of the stop starts the timer again.
- **I don't know where I fell asleep.** The night shows in the listening history, with a way back to where the listener
  was last awake.

## Open

- **With short chapters, end of chapter stops too soon.** [#1421]
- **Pressing play after the timer stopped plays on without a timer.** [#2233]
- **Setting the timer means opening the app.** [#3410]

## Won't solve

- **A bookmark for every night:** bookmarks are what the listener saved, and the night is in the history ([#3903]).
- **A new timer on every play, at any time of day:** too specific for everyone ([#535]).

[#535]: https://github.com/PaulWoitaschek/Voice/issues/535
[#1421]: https://github.com/PaulWoitaschek/Voice/discussions/1421
[#2233]: https://github.com/PaulWoitaschek/Voice/discussions/2233
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3903]: https://github.com/PaulWoitaschek/Voice/pull/3903
