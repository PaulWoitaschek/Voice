package voice.core.data.playback

import android.net.Uri

public interface RemoteAuthHeaderProvider {

  public fun headersFor(uri: Uri): Map<String, String>?
}
