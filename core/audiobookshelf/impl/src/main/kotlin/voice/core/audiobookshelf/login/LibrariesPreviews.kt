package voice.core.audiobookshelf.login

import android.content.Context
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.Request
import retrofit2.HttpException
import voice.core.audiobookshelf.AudiobookshelfLibrary
import voice.core.audiobookshelf.LibrariesPreview
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.logging.api.Logger

@Inject
internal class LibrariesPreviews(
  private val http: AudiobookshelfHttp,
  private val context: Context,
) {

  suspend fun load(
    account: Account,
    stored: Boolean,
  ): LibrariesPreview? =
    withContext(Dispatchers.IO) {
      val api =
        if (stored) {
          http.authenticatedApi(account.serverUrl)
        } else {
          http.withToken(account.serverUrl, account.accessToken)
        }
      try {
        val libraries = api.libraries().libraries.filter { it.mediaType == "book" }
        val firstItems = libraries.associate { library ->
          library.id to api.libraryItems(libraryId = library.id, limit = 8, page = 0)
        }
        val covers =
          firstItems.values
            .flatMap { it.results }
            .filter { it.media.coverPath != null }
            .take(5)
            .mapNotNull { item -> downloadCover(account, item.id, stored) }
        LibrariesPreview(
          libraries =
            libraries.map { library ->
              AudiobookshelfLibrary(
                id = library.id,
                name = library.name,
                bookCount = firstItems[library.id]?.total ?: 0,
              )
            },
          covers = covers,
        )
      } catch (_: IOException) {
        null
      } catch (e: HttpException) {
        Logger.w(e, "Could not load the libraries: HTTP ${e.code()}")
        null
      } catch (e: SerializationException) {
        Logger.w(e, "Could not load the libraries")
        null
      }
    }

  private fun downloadCover(
    account: Account,
    itemId: String,
    stored: Boolean,
  ): File? {
    val folder = File(context.cacheDir, "audiobookshelfPreview").apply { mkdirs() }
    val file = File(folder, "$itemId.jpg")
    if (file.exists()) return file
    val url = "${account.serverUrl}api/items/$itemId/cover?width=400&format=jpeg"
    val request =
      if (stored) {
        Request.Builder().url(url).build()
      } else {
        Request.Builder().url(url).header("Authorization", "Bearer ${account.accessToken}").build()
      }
    val client = if (stored) http.authenticatedClient else http.client
    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        file.outputStream().use { output -> response.body.byteStream().copyTo(output) }
        file
      }
    } catch (_: IOException) {
      null
    }
  }
}
