package voice.features.folderPicker.addcontent

import android.net.Uri
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import java.net.URLEncoder
import kotlinx.coroutines.launch
import voice.core.analytics.api.Analytics
import voice.core.audiobookshelf.AudiobookshelfFeatureFlag
import voice.core.audiobookshelf.ServerAccount
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
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
  private val audiobookshelfFeatureFlag: AudiobookshelfFeatureFlag,
  private val serverAccount: ServerAccount,
  private val analytics: Analytics,
  private val helpSurveyUrlFeatureFlag: FolderHelpSurveyUrlFeatureFlag,
  private val appInfoProvider: AppInfoProvider,
  dispatcherProvider: DispatcherProvider,
  @Assisted private val origin: Origin,
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

  internal val canConnectAudiobookshelf: Boolean
    get() = audiobookshelfFeatureFlag.get()

  internal fun onFolderPickerCancel() {
    analytics.event("folder_picker_cancelled")
  }

  internal fun onHelpAnswer(answer: FolderHelpAnswer) {
    analytics.event("folder_help_answer", mapOf("answer" to answer.value()))
  }

  internal fun openLibriVox() {
    navigator.goTo(Destination.Website("https://librivox.org"))
  }

  internal val canOpenHelpSurvey: Boolean
    get() = helpSurveyUrlFeatureFlag.get().isNotEmpty()

  internal fun openHelpSurvey(answer: FolderHelpAnswer?) {
    val url =
      helpSurveyUrlFeatureFlag.get().ifEmpty {
        return
      }
    // named like the hidden fields of the Tally form, which fills them from the query
    val params =
      listOfNotNull(
          answer?.let { "answer" to it.value() },
          "app_version" to appInfoProvider.versionName,
        )
        .joinToString(separator = "&") { (key, value) ->
          "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
    val separator = if ('?' in url) '&' else '?'
    navigator.goTo(Destination.Website("$url$separator$params"))
  }

  private fun FolderHelpAnswer.value(): String {
    return when (this) {
      FolderHelpAnswer.NoFiles -> "no_files"
      FolderHelpAnswer.NotFound -> "not_found"
      FolderHelpAnswer.OtherApp -> "other_app"
    }
  }

  private val scope = MainScope(dispatcherProvider)

  // a connected server is managed in its settings, connecting another one there signs it out first
  internal fun connectAudiobookshelf() {
    scope.launch {
      navigator.goTo(
        if (serverAccount.isConnected()) {
          Destination.AudiobookshelfSettings
        } else {
          Destination.AudiobookshelfLogin(origin)
        },
      )
    }
  }

  internal fun back() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(origin: Origin): AddContentViewModel
  }
}
