package voice.documentfile

import android.content.Context
import android.net.Uri
import com.squareup.anvil.annotations.ContributesBinding
import voice.common.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class RealCachedDocumentFileFactory
@Inject constructor(private val context: Context) : CachedDocumentFileFactory {
  override fun create(uri: Uri): CachedDocumentFile {
    return RealCachedDocumentFile(context = context, uri = uri, preFilledContent = null)
  }
}
