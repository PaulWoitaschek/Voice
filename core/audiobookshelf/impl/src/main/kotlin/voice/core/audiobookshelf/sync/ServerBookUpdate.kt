package voice.core.audiobookshelf.sync

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import voice.core.audiobookshelf.ServerBookEdit
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsAuthor
import voice.core.audiobookshelf.api.AbsSeries
import voice.core.audiobookshelf.api.AudiobookshelfApi
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
internal class ServerBookUpdate(
  private val http: AudiobookshelfHttp,
  private val accountStore: AccountStore,
  private val bookRepository: BookRepository,
) {

  suspend fun update(
    bookId: BookId,
    edit: ServerBookEdit,
  ): ServerEdit {
    val itemId = bookId.itemId ?: return ServerEdit.Failed
    val account = accountStore.data.first()?.takeUnless { it.needsLogin } ?: return ServerEdit.Failed
    val title = edit.title?.trim()?.takeIf { it.isNotEmpty() }
    // Voice shows the authors of a server book joined by commas
    val authors = edit.author?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    return try {
      val api = http.authenticatedApi(account.serverUrl)
      val series = edit.series?.let { series(api, itemId, bookId, name = it, part = edit.part.orEmpty()) }
      val response = api.updateMedia(itemId, MediaUpdate(MetadataUpdate(title, authors?.map(::AbsAuthor), series)))
      when {
        response.isSuccessful -> {
          bookRepository.updateBook(bookId) { content ->
            var metadata = content.metadata
            if (title != null) metadata = metadata.copy(name = title)
            if (authors != null) metadata = metadata.copy(author = authors.joinToString().ifEmpty { null })
            if (series != null) {
              val first = series.firstOrNull()
              metadata = metadata.copy(series = first?.name, part = first?.sequence)
            }
            content.copy(metadata = metadata)
          }
          ServerEdit.Saved
        }
        response.code() == 403 -> ServerEdit.NotAllowed
        else -> {
          Logger.w("Could not save the book: HTTP ${response.code()}")
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

  private suspend fun series(
    api: AudiobookshelfApi,
    itemId: String,
    bookId: BookId,
    name: String,
    part: String,
  ): List<AbsSeries> {
    val trimmed = name.trim().takeIf { it.isNotEmpty() }
    val edited = trimmed?.let { AbsSeries(it, sequence = part.trim().takeIf(String::isNotEmpty)) }
    // Voice shows only one series, and the server would drop the others if they were left out
    val current = api.items(BatchGetRequest(listOf(itemId))).libraryItems.singleOrNull()
      ?.media?.metadata?.series.orEmpty()
    // with none shown, a series the server got since the last sync isn't the listener's to replace
    val replaced = bookRepository.get(bookId)?.content?.metadata?.series?.let(current::shown)
    val others = current.filter { it != replaced && !it.name.equals(trimmed, ignoreCase = true) }
    return listOfNotNull(edited) + others
  }
}
