# Outside the app

Listeners control their book from headphones, the lock screen, the car and the home screen, without opening Voice.

## Solved

- **The car doesn't remember my book.** Android Auto has the current book ready when it connects. The F-Droid build
  has no Android Auto.
- **The car's skip buttons look like track buttons.** Rewind and fast forward have skip icons.
- **Headphone and car buttons lose my place.** Next and previous skip by the skip amount everywhere outside the app.
- **The lock screen slider loses my place.** It only covers the current chapter, and a setting turns it off.
- **My book isn't on my home screen.** A Now playing widget, down to 1×1, and a Shelf widget with the books in
  progress. As space runs out, Now playing keeps play first, then the cover, the title, rewind and fast forward.
- **Automations have to open Voice.** Tasker can play, pause, skip and start a book.

## Open

- **I can't change the speed in the car.** [#1878]
- **I can't play my book on a speaker or TV.** [#1317]
- **I can't listen on my watch without my phone.** [#1749]

## Won't solve

- **Next and previous change the chapter:** seeking matters far more than chapters ([#3846]).
- **Choosing what each headphone press does:** too specific ([#456]).
- **Jumping to a chapter by voice:** a misheard request could lose the listener's place ([#231]).
- **Controlling Voice through the phone's Assistant:** only players on Google's allowlist can be controlled ([#1071]).

[#231]: https://github.com/PaulWoitaschek/Voice/issues/231
[#456]: https://github.com/PaulWoitaschek/Voice/issues/456
[#1071]: https://github.com/PaulWoitaschek/Voice/issues/1071
[#1317]: https://github.com/PaulWoitaschek/Voice/discussions/1317
[#1749]: https://github.com/PaulWoitaschek/Voice/discussions/1749
[#1878]: https://github.com/PaulWoitaschek/Voice/discussions/1878
[#3846]: https://github.com/PaulWoitaschek/Voice/issues/3846
