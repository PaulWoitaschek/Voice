package voice.app.scanner

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import voice.documentfile.CachedDocumentFile
import voice.logging.core.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHasStoragePermissionBug
@Inject constructor(private val context: Context) {

  private val _hasBug = MutableStateFlow(false)
  val hasBug: StateFlow<Boolean> get() = _hasBug

  suspend fun checkForBugAndSet(probeFile: CachedDocumentFile): Boolean {
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
        Logger.e(e, "Probing for permission failed!")
        "com.android.externalstorage has no access" in (e.message ?: "")
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Logger.e(e, "Probing for permission failed!")
        false
      }
    }
  }
}
