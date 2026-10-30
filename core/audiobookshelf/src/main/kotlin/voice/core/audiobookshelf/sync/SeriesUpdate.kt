package voice.core.audiobookshelf.sync

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsSeries
import voice.core.audiobookshelf.api.BatchGetRequest
import voice.core.audiobookshelf.api.MediaUpdate
import voice.core.audiobookshelf.api.MetadataUpdate
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.itemId
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import java.io.IOException

@Inject
internal class SeriesUpdate(
  private val http: AudiobookshelfHttp,
  private val accountStore: AccountStore,
  private val bookRepository: BookRepository,
) {

  suspend fun update(
    bookId: BookId,
    series: String,
    part: String,
  ): ServerEdit {
    val itemId = bookId.itemId ?: return ServerEdit.Failed
    val account = accountStore.data.first()?.takeUnless { it.needsLogin } ?: return ServerEdit.Failed
    val name = series.trim().takeIf { it.isNotEmpty() }
    val edited = name?.let { AbsSeries(it, sequence = part.trim().takeIf(String::isNotEmpty)) }
    return try {
      val api = http.authenticatedApi(account.serverUrl)
      // Voice shows only one series, and the server would drop the others if they were left out
      val current = api.items(BatchGetRequest(listOf(itemId))).libraryItems.singleOrNull()
        ?.media?.metadata?.series.orEmpty()
      // with none shown, a series the server got since the last sync isn't the listener's to replace
      val replaced = bookRepository.get(bookId)?.content?.metadata?.series?.let(current::shown)
      val others = current.filter { it != replaced && !it.name.equals(name, ignoreCase = true) }
      val updated = listOfNotNull(edited) + others
      val response = api.updateMedia(itemId, MediaUpdate(MetadataUpdate(updated)))
      when {
        response.isSuccessful -> {
          val first = updated.firstOrNull()
          bookRepository.updateBook(bookId) {
            it.copy(metadata = it.metadata.copy(series = first?.name, part = first?.sequence))
          }
          ServerEdit.Saved
        }
        response.code() == 403 -> ServerEdit.NotAllowed
        else -> {
          Logger.w("Could not save the series: HTTP ${response.code()}")
          ServerEdit.Failed
        }
      }
    } catch (_: IOException) {
      ServerEdit.Failed
    } catch (e: HttpException) {
      Logger.w(e, "Could not read the book from the server: HTTP ${e.code()}")
      ServerEdit.Failed
    } catch (e: SerializationException) {
      Logger.w(e, "The server answered in an unexpected way")
      ServerEdit.Failed
    }
  }
}
