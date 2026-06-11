package voice.features.support

sealed interface SupportBackendState {
  data object Free : SupportBackendState

  data object PlayUnavailable : SupportBackendState
}
