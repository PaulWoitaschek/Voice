package voice.documentfile

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.common.AppScope

@ContributesBinding(AppScope::class)
@Inject
class RealCachedDocumentFileFactory(private val context: Context) : CachedDocumentFileFactory {
  override fun create(uri: Uri): CachedDocumentFile {
    return RealCachedDocumentFile(context = context, uri = uri, preFilledContent = null)
  }
}
