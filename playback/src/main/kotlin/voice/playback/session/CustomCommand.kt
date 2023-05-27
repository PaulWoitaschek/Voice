package voice.playback.session

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import voice.playback.misc.Decibel

@Serializable
internal sealed interface CustomCommand {

  @Serializable
  data object ForceSeekToNext : CustomCommand

  @Serializable
  data object ForceSeekToPrevious : CustomCommand

  @Serializable
  data class SetSkipSilence(val skipSilence: Boolean) : CustomCommand

  @Serializable
  data class SetGain(val gain: Decibel) : CustomCommand

  companion object {

    const val CustomCommandAction = "voiceCommandAction"
    internal const val CustomCommandExtra = "voiceCommandExtra"
    internal fun parse(command: SessionCommand, args: Bundle): CustomCommand? {
      if (command.customAction != CustomCommandAction) {
        return null
      }
      val json = args.getString(CustomCommandExtra) ?: return null
      return Json.decodeFromString(serializer(), json)
    }
  }
}

internal sealed class PublishedCustomCommand {

  abstract val sessionCommand: SessionCommand

  object Sleep : PublishedCustomCommand() {
    override val sessionCommand: SessionCommand = SessionCommand("voice.sleep", Bundle.EMPTY)
  }
}

internal fun MediaController.sendCustomCommand(command: CustomCommand) {
  val json = Json.encodeToString(CustomCommand.serializer(), command)
  sendCustomCommand(
    SessionCommand(CustomCommand.CustomCommandAction, Bundle.EMPTY),
    Bundle().apply {
      putString(CustomCommand.CustomCommandExtra, json)
    },
  )
}
