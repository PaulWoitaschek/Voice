plugins {
  alias(libs.plugins.compose.compiler) apply false
  id("voice.ktlint")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}
