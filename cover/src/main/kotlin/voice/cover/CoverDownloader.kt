package voice.cover

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okio.sink
import voice.logging.core.Logger
import java.io.File
import java.io.IOException
import java.util.UUID

@Inject
class CoverDownloader(
  private val client: OkHttpClient,
  private val context: Context,
) {

  internal suspend fun download(url: String): File? {
    val tempFolder = File(context.cacheDir, "coverDownload")
      .apply {
        deleteRecursively()
        mkdirs()
      }
    val request = Request.Builder()
      .url(url)
      .build()
    val response = try {
      client.newCall(request).executeAsync()
    } catch (e: IOException) {
      Logger.w(e, "Failed to download cover from $url")
      return null
    }
    return withContext(Dispatchers.IO) {
      try {
        response.body.source().use { source ->
          // select a random name so on updating this, the old image is not cached
          val file = File(tempFolder, UUID.randomUUID().toString())
          file.sink().use { sink ->
            source.readAll(sink)
          }
          file
        }
      } catch (e: IOException) {
        Logger.w(e, "Failed to save cover from $url")
        null
      }
    }
  }
}
