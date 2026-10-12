package voice.navigation

import kotlinx.serialization.json.Json
import voice.core.data.BookId
import kotlin.test.Test
import kotlin.test.assertEquals

class DestinationJsonTest {

  @Test
  fun `a debug back stack decodes destinations by their class name`() {
    val backStack = Json.decodeFromString<List<Destination.Compose>>(
      """
      [
        {"type": "voice.navigation.Destination.BookOverview"},
        {"type": "voice.navigation.Destination.Playback", "bookId": "book"}
      ]
      """,
    )
    assertEquals(
      expected = listOf(Destination.BookOverview, Destination.Playback(BookId("book"))),
      actual = backStack,
    )
  }
}
