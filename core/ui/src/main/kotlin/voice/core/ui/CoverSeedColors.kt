@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package voice.core.ui

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.materialkolor.ktx.themeColorOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.data.store.CoverSeedColorsStore
import voice.core.logging.api.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Provided by the activity. Without it (e.g. in previews), covers don't re-theme anything. */
val LocalCoverSeedColors = staticCompositionLocalOf<CoverSeedColors?> { null }

/**
 * The theme seed colors extracted from covers, keyed by cover url.
 *
 * They are persisted, so after a restart a cover is themed from its very first frame instead of
 * showing the default colors while it is analyzed again. Covers are stored under unique file names,
 * so a changed cover always gets a new key.
 */
@SingleIn(AppScope::class)
@Inject
class CoverSeedColors(
  private val application: Application,
  @CoverSeedColorsStore
  private val store: DataStore<Map<String, Int>>,
  private val scope: CoroutineScope,
) {

  // Color.Unspecified marks covers without a usable color
  private val memory = ConcurrentHashMap<String, Color>()

  private val restored = scope.async(start = CoroutineStart.LAZY) {
    val colors = try {
      store.data.first()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // only a cache: without it, covers are just analyzed again
      Logger.w(e, "Can't restore the cover colors")
      emptyMap()
    }
    colors.forEach { (cover, argb) ->
      memory.putIfAbsent(cover, if (argb == NO_COLOR) Color.Unspecified else Color(argb))
    }
    scope.launch(Dispatchers.IO) {
      persist { stored -> stored.filterKeys(::coverExists) }
    }
  }

  /** Loads the colors of earlier sessions. Await this before showing covers to avoid a color flash. */
  suspend fun restore() {
    restored.await()
  }

  fun cached(cover: String): Color? = memory[cover]

  /** The seed for [cover], extracting it if it is not known yet. Null if the cover can't be loaded. */
  suspend fun load(cover: String): Color? {
    restore()
    memory[cover]?.let { return it }
    val request = ImageRequest.Builder(application)
      .data(cover)
      .size(128)
      .allowHardware(false)
      // its key would be the full size cover's, which this small software bitmap would replace
      .memoryCachePolicy(CachePolicy.DISABLED)
      .build()
    // failed loads are not cached so they are retried the next time
    val result = application.imageLoader.execute(request) as? SuccessResult
      ?: return null
    val color = withContext(Dispatchers.Default) {
      result.drawable.toBitmap().asImageBitmap().themeColorOrNull()
    } ?: Color.Unspecified
    memory[cover] = color
    scope.launch {
      persist { stored ->
        stored + (cover to if (color.isSpecified) color.toArgb() else NO_COLOR)
      }
    }
    return color
  }

  private suspend fun persist(transform: (Map<String, Int>) -> Map<String, Int>) {
    try {
      store.updateData(transform)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // e.g. a full disk; the colors stay in memory for this session
      Logger.w(e, "Can't persist the cover colors")
    }
  }

  private fun coverExists(cover: String): Boolean {
    val uri = cover.toUri()
    val path = uri.path
    return uri.scheme != "file" || path == null || File(path).exists()
  }
}

// theme colors are opaque, so fully transparent black can't collide with an extracted color
private const val NO_COLOR = 0
