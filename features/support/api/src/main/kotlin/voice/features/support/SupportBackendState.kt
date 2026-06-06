package voice.features.support

sealed interface SupportBackendState {
  data class Free(val supporterBadgeVisible: Boolean) : SupportBackendState

  data object PlayUnavailable : SupportBackendState
}
