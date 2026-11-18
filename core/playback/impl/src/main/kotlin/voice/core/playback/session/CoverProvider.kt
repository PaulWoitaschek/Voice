package voice.core.playback.session

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.runBlocking

private const val CACHED_PATH = "cache"

// Android Auto only reads artwork from content uris, so server covers go through the image cache
class CoverProvider : FileProvider() {

  // Android can call a provider before Application.onCreate sets up the image loader on the main
  // thread
  private val appStarted = lazy {
    val started = CountDownLatch(1)
    Handler(Looper.getMainLooper()).post(started::countDown)
    started.await()
  }

  override fun openFile(
    uri: Uri,
    mode: String,
  ): ParcelFileDescriptor? {
    val cover = uri.cachedCover() ?: return super.openFile(uri, mode)
    val context = requireNotNull(context)
    // a pipe frees the binder thread while the cover loads from the server
    return openPipeHelper(uri, "image/jpeg", null, cover) { output, _, _, _, _ ->
      appStarted.value
      FileOutputStream(output.fileDescriptor).use { runBlocking { writeCover(context, cover, it) } }
    }
  }

  override fun getType(uri: Uri): String? {
    return if (uri.cachedCover() != null) "image/jpeg" else super.getType(uri)
  }
}

internal suspend fun writeCover(
  context: Context,
  cover: String,
  output: OutputStream,
) {
  val imageLoader = context.imageLoader
  val request =
    ImageRequest.Builder(context)
      .data(cover)
      .diskCacheKey(cover)
      .size(1)
      .memoryCachePolicy(CachePolicy.DISABLED)
      .build()
  if (imageLoader.execute(request) !is SuccessResult) return
  try {
    imageLoader.diskCache?.openSnapshot(cover)?.use { snapshot ->
      snapshot.data.toFile().inputStream().use { it.copyTo(output) }
    }
  } catch (_: IOException) {
    // the other app stopped reading, an exception here would end Voice
  }
}

internal fun cachedCoverUri(
  authority: String,
  cover: String,
): Uri =
  Uri.Builder()
    .scheme("content")
    .authority(authority)
    .appendPath(CACHED_PATH)
    .appendPath(cover)
    .build()

private fun Uri.cachedCover(): String? =
  pathSegments.takeIf { it.size == 2 && it[0] == CACHED_PATH }?.get(1)
