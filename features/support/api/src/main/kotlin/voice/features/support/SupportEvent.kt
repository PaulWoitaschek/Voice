package voice.features.support

sealed interface SupportEvent {

  /** The payment went through. */
  data object ThankYou : SupportEvent

  /** The payment is still processing, e.g. when paying in cash. */
  data object Pending : SupportEvent

  data object Failed : SupportEvent
}
