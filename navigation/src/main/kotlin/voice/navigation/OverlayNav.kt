package voice.navigation

import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata

/**
 * Shows an entry above the previous ones without a container of its own. For flows that bring their own sheets and
 * move from one to the next, which a single [BottomSheetNav] sheet can't hold.
 */
object OverlayNav {
  object OverlayKey : NavMetadataKey<Boolean>

  fun overlay() = metadata {
    put(OverlayKey, true)
  }
}
