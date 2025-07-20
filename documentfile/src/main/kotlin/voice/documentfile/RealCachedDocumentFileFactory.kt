package voice.documentfile

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import voice.common.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealCachedDocumentFileFactory
@Inject constructor(private val context: Context) : CachedDocumentFileFactory {
  override fun create(uri: Uri): CachedDocumentFile {
    return RealCachedDocumentFile(context = context, uri = uri, preFilledContent = null)
  }
}
