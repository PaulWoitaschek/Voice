package voice.scanner

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import voice.documentfile.CachedDocumentFile
import voice.logging.core.Logger

@Inject
public class DeviceHasStoragePermissionBug(private val context: Context) {

  private val _hasBug = MutableStateFlow(false)
  public val hasBug: StateFlow<Boolean> get() = _hasBug

  internal suspend fun checkForBugAndSet(probeFile: CachedDocumentFile): Boolean {
    return deviceHasPermissionBug(probeFile)
      .also {
        Logger.d("update hasBug to $it")
        _hasBug.emit(it)
      }
  }

  private suspend fun deviceHasPermissionBug(probeFile: CachedDocumentFile): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        context.contentResolver.openInputStream(probeFile.uri)?.close()
        false
      } catch (e: SecurityException) {
        // https://issuetracker.google.com/issues/258270138
        Logger.w(e, "Probing for permission failed!")
        "com.android.externalstorage has no access" in (e.message ?: "")
      } catch (e: Exception) {
        if (e is CancellationException) ensureActive()
        Logger.w(e, "Probing for permission failed!")
        false
      }
    }
  }
}
