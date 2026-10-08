package voice.features.support

sealed interface SupportEvent {

  data object ThankYou : SupportEvent

  data object Pending : SupportEvent

  data object Failed : SupportEvent
}
