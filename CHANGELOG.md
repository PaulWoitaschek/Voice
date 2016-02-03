# Next Version

# v2.5.1
* Updated dependencies
* Removed guava and decreased app size
* Safe Bookmarks unrelated to books. This way they will survive a book deletion etc.
* MediaPlayer stability improved
* Translations updated

# till v2.5.0.10
No changelog

# v2.4.3 Oh sweet sweet kotlin
* Everything rewritten in Kotlin
* Reactive Programming (rxJava)
* External storage on MarshMallow fixed
* Software architecture rethought and many components decoupled
* Optimized images
* Higher test coverage
* Updated dependencies
* File sorting optimized
* Play on cover only through double click
* Dagger 2 used as Dependency Injection Framework
* Made all data classes immutable
* Added Hindi translation
* Added afrikaans translation
* Added swedish translation
* Added chinese translation
* Updated translations

# v2.4.2
* Added bulgarian translation
* Prevent time overlapping the progress view in the list
* Improved general translations (support for plurals)
* Minor performance improvements

# v2.4.1
* Updated for android 6
* Fixed crashed on new tablet installations
* Fixed rare crashes

# v2.4.0.1 Hotfix
* Fixed tablet orientation problems
* Fixed audio track crash
* Fixed scene transitions after rotation

# v2.4.0 Detail view
* Switch between the grid and a detailed view with a global progress
* Improved accesibility
* Performance greatly improved
* More ripples

# v2.3.0
* Dont auto rewind when pausing due to interruption
* Enabled scene transitions for cover
* Enabled scene transitions for fab
* More ripples
* Translations updated
* Have the media style notification for pre lollipop as well
* Performance improved
* Removed snackbar and have a timer for the sleep timer instead.

# v2.2.3.1 Hotfix
* Fixed base color tinting
* Fixed a crash where a wrong attr was referenced
* Make a .nomedia file insted of a folder

# v2.2.3
* Added mp3package as file extension
* Add an option to hide the book from music players
* Object creation decreased
* Performance increased
* Correctly collapse FAM
* Tint all the icons insted of having multiple icons in different colors
* Replaces CardView with normal grid
* Add ripples to grid (book shelf)
* Disable buggy transitions
* Replaced Sonic NDK with Java version
* Minor fixes

# v2.2.2 Fixes and Snackbar Theming
* Repairs database corruption by a previous update
* Theme snackbar. Thanks to dark-seizo!
* Minor improvements

# v2.2.1

## Changes
* Correctly check for existing bookmarks if book has changed
* Fixed issue with name recognition of chapter and books
* Use snackbar instead of toast for the sleep timer
* Added Brasilian Portugeese translation
* Updated translations

# v2.2

## Changes
* More metadata (like author)
* Quick return to current book
* Correct toolbar menu colors set
* PlayPauseDrawable improved
* Highlights selectet item in spinner
* Revert speed fix causing problems
* Blacklist some devices that have a bug in MediaExtractor
* Add awb to audio types
* Correctly update books
* Many performance improvements
* Synchronization improved

# v2.1.9.1

## Changes
* More intelligent track naming

# v2.1.9

## Changes
* Audio quality improved. No more weird sound on seeking
* Read metatags for chapter and book title

# v2.1.8

## Changes
* Bug about database upgrading from previous version has been fixed
* Checks for non existing book
* Added duration and genre as metainfo

# v2.1.7

## Changes
* Added polish translation
* Settings in book grid has ripples now
* Tint seekbar on pre lollipop
* Shorter times will be formated to 01:05 instead of 0:01:05
* Removed stop after track for sleep timer
* Multi line spinner in BokoPlay for better overview over books
* Book Grid layout improved
* Bookmarks have ripples now
* Reenter transitions of playbutton now handled correctly
* Hide books instead of deleting them so readding a book known previously will restore its state
* Respect external storage state on adding / deleting books

# v2.1.6.1

## Changes
* Bookmark title uses cap at start
* Autocorrect on bookmark title
* Ukraninan updated
* Improved back navigation

# v2.1.6

## Additions
* Use cover also as play button
* Bookmarks editable from BookShelf

## Fixes
* Fixes refreshing books which would lead to crash if book was empty after searching for update

# v2.1.5 Support

## Additions
* Added a support menu
* Added option to donate
* Added hint how book adding works

# v2.1.4 Details

## Additions
* xxxhdpi icons complete
* Ukrainan language added. Thanks to [gladk](https://github.com/gladk)
* Performance greatly improved
* Translations updated

## Fixes
* Wrong theming on older Android versions fixed
* Threading issues fixed
* Correctly hide and show fab
* Memory used minimized

# v2.1.3 Details

## Additions
* Added setting to set a bookmark on sleep timer
* Bookmark icon on higher priority
* Replaced miniplayer widget with fab
* Improved transitions
* 
## Fixes
* Shows correct error message on corrupt files
* Threading issues fixed
* Crash on database upgrade fixed
* Transition on Coverreplacement omitted

# v2.1.2.1 Stability

We cant store positions to internal storage since on Android 4.4 there is no permission to write to sd card.
So we write back to db.

## Additions
* Added greek. Thanks to beonex (Wasilis Mandratzis-Walz)!
* Added italian. Thanks to  Lurtz (Davide Andreotti)!
* Minimized sound on rewind
* On Lollipop: Dark navigation color on dark theme
* Adds books by alphabet
* Adds covers after books

## Fixes
* Dont hold listener to headset controls after we stopped
* Threading issues fixed

# v2.1.1 Fancy Pants

## Additions
* Play buttons will transform with animation
* Press back while fam is shown will collapse it

## Fixes
* Little threading issue fixed

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

