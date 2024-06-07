plugins {
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.android.app) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  id("voice.ktlint")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}
