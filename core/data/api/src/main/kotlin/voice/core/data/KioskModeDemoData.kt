package voice.core.data

public object KioskModeDemoData {
  public const val COVER_URL: String = "https://raw.githubusercontent.com/PaulWoitaschek/Voice/" +
    "main/Images/covers/Dream%20in%20a%20boat/cover.jpg"

  public val demoAudiobooks: List<DemoAudiobook> = listOf(
    DemoAudiobook(
      id = BookId("echoes_of_tomorrow"),
      title = "Echoes of Tomorrow",
      author = "Daniel Hartwell",
      genre = "Science fiction",
      duration = "14:27:33",
      progress = 72,
      chapter = "Chapter 12: The Signal",
      currentPosition = "10:24:18",
      remaining = "4:03:15",
      bookmarks = listOf(
        DemoBookmark(title = "The first signal", timestamp = "02:14:08"),
        DemoBookmark(title = "Mira's theory", timestamp = "06:42:31"),
        DemoBookmark(title = "The hidden message", timestamp = "10:18:55"),
      ),
      coverUrl = COVER_URL,
    ),
    DemoAudiobook(
      id = BookId("the_bookshop_on_maple_street"),
      title = "The Bookshop on Maple Street",
      author = "Lena Morgan",
      genre = "Cozy mystery",
      duration = "8:19:47",
      progress = 48,
      chapter = "Chapter 8: After Closing",
      currentPosition = "3:59:52",
      remaining = "4:19:55",
      bookmarks = listOf(
        DemoBookmark(title = "The strange receipt", timestamp = "00:47:12"),
        DemoBookmark(title = "A clue in the window", timestamp = "03:18:44"),
        DemoBookmark(title = "After closing", timestamp = "04:02:09"),
      ),
      coverUrl = COVER_URL,
    ),
    DemoAudiobook(
      id = BookId("beyond_the_horizon"),
      title = "Beyond the Horizon",
      author = "Alexander Reeves",
      genre = "Adventure",
      duration = "11:05:22",
      progress = 33,
      chapter = "Chapter 5: The Old Map",
      currentPosition = "3:39:17",
      remaining = "7:26:05",
      bookmarks = listOf(
        DemoBookmark(title = "The old map", timestamp = "01:22:36"),
        DemoBookmark(title = "Crossing the ridge", timestamp = "03:41:10"),
        DemoBookmark(title = "The compass turns", timestamp = "05:08:29"),
      ),
      coverUrl = COVER_URL,
    ),
  )

  public val currentlyPlaying: CurrentlyPlaying = CurrentlyPlaying(
    bookId = BookId("echoes_of_tomorrow"),
    screenTitle = "Echoes of Tomorrow",
    screenAuthor = "by Daniel Hartwell",
    chapter = "Chapter 12: The Signal",
    position = "10:24",
    duration = "14:27",
  )

  public val bookmarkScreen: DemoBookmarkScreen = DemoBookmarkScreen(
    bookId = BookId("echoes_of_tomorrow"),
    title = "Bookmarks",
    items = listOf(
      DemoBookmark(title = "The first signal", timestamp = "02:14:08"),
      DemoBookmark(title = "Mira's theory", timestamp = "06:42:31"),
      DemoBookmark(title = "The hidden message", timestamp = "10:18:55"),
      DemoBookmark(title = "Return to this later", timestamp = "12:03:44"),
    ),
  )

  public val currentlyPlayingBook: DemoAudiobook = demoAudiobooks.single { it.id == currentlyPlaying.bookId }
}

public data class DemoAudiobook(
  public val id: BookId,
  public val title: String,
  public val author: String,
  public val genre: String,
  public val duration: String,
  public val progress: Int,
  public val chapter: String,
  public val currentPosition: String,
  public val remaining: String,
  public val bookmarks: List<DemoBookmark>,
  public val coverUrl: String,
)

public data class DemoBookmark(
  public val title: String,
  public val timestamp: String,
)

public data class CurrentlyPlaying(
  public val bookId: BookId,
  public val screenTitle: String,
  public val screenAuthor: String,
  public val chapter: String,
  public val position: String,
  public val duration: String,
)

public data class DemoBookmarkScreen(
  public val bookId: BookId,
  public val title: String,
  public val items: List<DemoBookmark>,
)
