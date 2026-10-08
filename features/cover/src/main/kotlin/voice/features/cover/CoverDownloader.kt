package voice.features.cover

import android.content.Context
import android.graphics.BitmapFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okio.sink
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException
import kotlin.uuid.Uuid

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
    if (!response.isSuccessful) {
      Logger.w("Failed to download cover from $url: ${response.code}")
      response.close()
      return null
    }
    return withContext(Dispatchers.IO) {
      try {
        response.body.source().use { source ->
          // select a random name so on updating this, the old image is not cached
          val file = File(tempFolder, Uuid.random().toString())
          file.sink().use { sink ->
            source.readAll(sink)
          }
          // some image urls lead to a web page instead
          if (file.isImage()) {
            file
          } else {
            Logger.w("Cover from $url is no image")
            file.delete()
            null
          }
        }
      } catch (e: IOException) {
        Logger.w(e, "Failed to save cover from $url")
        null
      }
    }
  }
}

private fun File.isImage(): Boolean {
  val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeFile(path, options)
  return options.outWidth > 0 && options.outHeight > 0
}
