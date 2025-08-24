plugins {
  alias(libs.plugins.compose.compiler) apply false
  id("voice.ktlint")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

dependencyAnalysis {
  useTypesafeProjectAccessors(true)
  abi{
    exclusions {
      ignoreInternalPackages()
      ignoreGeneratedCode()
    }
  }
  structure {
    bundle("media3") {
      includeGroup("androidx.media3")
    }
  }
}
