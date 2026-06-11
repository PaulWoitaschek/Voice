package voice.navigation

import androidx.compose.material3.ModalBottomSheetProperties
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata

object BottomSheetNav {
  object BottomSheetKey : NavMetadataKey<ModalBottomSheetProperties>

  fun bottomSheet(modalBottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()) = metadata {
    put(BottomSheetKey, modalBottomSheetProperties)
  }
}
