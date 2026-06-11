package voice.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata

@OptIn(ExperimentalMaterial3Api::class)
object BottomSheetNav {
  object BottomSheetKey : NavMetadataKey<ModalBottomSheetProperties>

  fun bottomSheet(modalBottomSheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()) = metadata {
    put(BottomSheetKey, modalBottomSheetProperties)
  }
}
