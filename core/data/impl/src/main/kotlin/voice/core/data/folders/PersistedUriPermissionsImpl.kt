package voice.core.data.folders

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
public class PersistedUriPermissionsImpl(private val context: Context) : PersistedUriPermissions {
  public override fun persistedUris(): Set<Uri> {
    return context.contentResolver.persistedUriPermissions.map { it.uri }.toSet()
  }
}
