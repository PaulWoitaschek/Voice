package deps

@Suppress("unused")
enum class DependencyStability(private val regex: Regex) {
  Dev(".*dev.*".toRegex()),
  Eap("eap".toRegex()),
  Milestone("M1".toRegex()),
  Alpha("alpha".toRegex()),
  Beta("beta".toRegex()),
  Rc("rc".toRegex()),
  Stable(".*".toRegex());

  companion object {
    @JvmStatic
    fun ofVersion(version: String): DependencyStability {
      return values().first {
        it.regex.containsMatchIn(version)
      }
    }
  }
}
