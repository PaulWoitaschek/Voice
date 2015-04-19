# v2.1.0 Single books back

## Additions
* New way to add single books. Containing material animations, enjoy them.
* Material Transitions!
* Ripples on button press for Lollipop +. Much better feedback on button pressing
* Czech translations updated
* Animate Play button
* Korean updated
* German updated

## Fixes
* Fixes rare crashes where media player died due to defect audio files
* Smaller fixes
* Performance improvements

# v2.0.9.1 Hotfix

## Fixes
* Correctly hide progressbar after first book has been added.

# v2.0.9 Translation & Hotfix

## Addition
* KO fully translated

## Fixes
* Typos fixed
* Threading issue fixed, so recycler should hopefully never crash again due to invalid state

# v2.0.8 Autorewind & Finetuning

## Additions
* Autorewind setting
* Settings grouped
* List layouts improved to match material specs
* Bookmark layout reworked
* Round button backgrounds (see when clicked)

## Fixes
* Load cover from storage only if there is enough free memory (RAM)
* Fixed a bug that caused skip not to work on large files
* Correctly initialized widget with new padding

# v2.0.7 Hotfix

## Fixes
* Fixed threading issue on MediaPlayer
* Fixed rare race condition error at BookShelf and BookPlay fragment 

# v2.0.6 Finetuning

## Additions
* Added wma
* Use correct colors and update widget correctly
* More intensive colors
* Removed Transitions for now since they caused many bugs
* FAB as play button
* Calculates the amount of columns according to device orientation on BookShelf

## Fixes
* Fixed a rare crash on defect music files
* Minor fixes

# v2.0.5 More Transitions

## Additions
* More transitions
* Layout improved
* Layout performance improved

## Fixes
* Fixed bug that caused replacement cover to be stored permanently

# v2.0.4 Material Transitions

## Additions
* Cover transitions
* BookShelf speed optimized
* No more manual ordering, sorts by name now
* Bookmarks with the same name sorted by natural ordering
* Deletes backup file on writing success

## Fixes
* Bug with RuntimeChanges on EditBookDialog fixed

# v2.0.3 Minor changes

## Changes
* Auto Bugreport opt-out now

## Fixes
* Minor bugs fixed

# v2.0.2 Minor changes

## Changes
* Reverted package name

# v2.0.1 Hotfix

## Fixes
* Fixed a rare crash after language change

# v2.0.0 Complete rewrite

There is a huge rewrite since the last release, so getting all the changes seems impossible. So I just name the biggest two.

## Change number one
You choose only your audiobook root now. Every folder in there (the main hieararchy) will be recognized as a single book. That means no more adding books. No more removing books. Just choose the folder and done.
## Change number two
The app stores the config for the books directly next to the book. That means you can move your folders, reinstall the app and the app will keep the books.

# v1.5.4

## Additions
* Now with bookmarks
* New Icons
* Automatically backs up preferences
* Completely rewritten underlaying mediaPlayer code
* Can now play .oga files
* Layout improvements
* Minimize data useage on cover loading
* Its now possible to use the replacement cover permanently
* Save speed for each book individually
* Performance improvements

## Fixes
* Some theming fixed
* Headset buttons should work now
* Correctly pauses on incoming notification
* Correctly pauses player on incoming call
* Some theming fixed
* Minor fixes

# v1.4.6.1

## Fixes
* Prevents media from resume playing after incoming notification when player was not pause.
* Smaller bugfixes

# v1.4.6

## Additions
* Album art on lockscreen for Lollipop
* Notification visually improved
* Confirmation on deleting book
* Swapping book to empty (last) position now possible
* Speed of book adding improved
* Performance improvements

## Fixes
* Only resumes after call if player really was playing before
* Cover crop weird border fixed
* Correctly inits spinner in player

# v1.4.5

## Additions
* Notification reworked
* Czech translation

## Fixes
* Crash in preferences fixed

# v1.4.4

## Additions
* Added m4b support
* Reworked sleep timer

## Fixes
* Home Up shown in Preferences now

# v1.4.3

## Additions
* Now with variable Playback speed for Android >= 4.1
* Floating actionbutton in book chooser

## Fixes
* Bluetooth should now be played correctly
* Bug with wrong sorting on file adding fixed

# v1.4.2

## Additions
* Added Lollipop controls
* Updated notification for lollipop

## Fixes
* Finally fixed crash on notification
* Reworked AudioPlayerService

# v1.4.1.1

## Fixes
* Hotfix: Crash on Notification

# v1.4.1

## Fixes
* Checks for defect audiofiles
* Fixes crash by notification
* Fixed crash when there were no covers found from the internet

# v1.4.0

## Improvements
* Now with material design
* New grid-based design
* Much much less memory needed on SD-Card
* Now its possible choose the part of the cover you want to use. No more ugly cropping.
* Now with Korean translation. Credits to Josh Graham
* More control from notification

## Fixes
* Player should now always keep track of the right position
* Keyboard should no longer pop up automatically when adding/editing a book
* Should now properly resume on call ended
* Many, many smaller fixes

# v1.3.0

## Improvements
* Can change book-sort-order now
* Automatically downloads missing covers
* Added possiblity to edit book afterwards
* Faster Database access
* Position changer improved
* Now shows the whole filename of media to see the file-end
* Smaller size
* Adding book-progress reworked
* Smaller bugfixes

# v1.2.5.1

## Additions
* Now with russian translation. All credits to: Utrobin Mikhail

# v1.2.5

## Additions
* Loads covers also on eternet etc.
* Better lockscreen information
* More information in notification now

## Bugfixes
* Fixed a bug where the textfield made the keyboard pop up
* Fixes some bugs considering lockscreen
* Fixes a layout bug with strange marked areas when deleting book
* Additional checks for mounted folders to look for audiobooks

