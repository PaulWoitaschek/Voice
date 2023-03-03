package voice.playback.session

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject

class ImageFileProvider
@Inject constructor(
  private val application: Application,
) {

  internal fun uri(file: File): Uri {
    return FileProvider.getUriForFile(
      application,
      application.packageName + ".coverprovider",
      file,
    )
  }
}
