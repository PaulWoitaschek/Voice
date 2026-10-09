package voice.features.folderPicker.addcontent

import android.net.Uri
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.featureflag.AudiobookshelfFeatureFlagQualifier
import voice.core.featureflag.FeatureFlag
import voice.features.folderPicker.folderPicker.FileTypeSelection
import voice.navigation.Destination
import voice.navigation.Destination.OnboardingCompletion
import voice.navigation.Destination.SelectFolderType
import voice.navigation.Navigator
import voice.navigation.Origin

@AssistedInject
class AddContentViewModel(
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
  @AudiobookshelfFeatureFlagQualifier
  private val audiobookshelfFeatureFlag: FeatureFlag<Boolean>,
  @Assisted
  private val origin: Origin,
) {

  internal fun add(
    uri: Uri,
    type: FileTypeSelection,
  ) {
    when (type) {
      FileTypeSelection.File -> {
        audiobookFolders.add(uri, FolderType.SingleFile)
        when (origin) {
          Origin.Default -> {
            navigator.setRoot(Destination.BookOverview)
          }
          Origin.Onboarding -> {
            navigator.goTo(OnboardingCompletion)
          }
        }
      }
      FileTypeSelection.Folder -> {
        navigator.goTo(
          SelectFolderType(
            uri = uri,
            origin = origin,
          ),
        )
      }
    }
  }

  internal val canConnectAudiobookshelf: Boolean get() = audiobookshelfFeatureFlag.get()

  internal fun connectAudiobookshelf() {
    navigator.goTo(Destination.AudiobookshelfLogin(origin))
  }

  internal fun back() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(origin: Origin): AddContentViewModel
  }
}
