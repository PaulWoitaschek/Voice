plugins {
  alias(libs.plugins.compose.compiler) apply false
  id("voice.ktfmt")
}

listOf("spotlessCheck", "spotlessApply").forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(gradle.includedBuild("plugins").task(":$taskName"))
  }
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}
