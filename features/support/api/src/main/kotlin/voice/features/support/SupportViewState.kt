package voice.features.support

data class SupportViewState(val backendState: SupportBackendState) {
  companion object {
    fun preview() = SupportViewState(
      backendState = SupportBackendState.Free,
    )
  }
}
