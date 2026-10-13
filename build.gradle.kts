plugins {
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.dependencyAnalysis)
  id("voice.ktlint")
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

dependencyAnalysis {
  useTypesafeProjectAccessors(true)
  issues {
    all {
      onAny {
        severity("fail")
      }
      onUsedTransitiveDependencies {
        // only reached through Kotlin metadata of types inferred from annotated Java APIs
        exclude("org.jspecify:jspecify")
      }
    }
  }
  reporting {
    printBuildHealth(true)
    postscript("Apply the advice with ./gradlew fixDependencies, then review the changed build files.")
  }
  structure {
    ignoreKtx(true)
    // libraries split into several artifacts, of which modules only declare the entry point
    bundle("compose") {
      include("^androidx\\.compose\\..*")
      includeGroup("androidx.graphics")
    }
    bundle("activity") {
      includeGroup("androidx.activity")
    }
    bundle("lifecycle") {
      includeGroup("androidx.lifecycle")
    }
    bundle("coil") {
      includeGroup("io.coil-kt")
    }
    bundle("media3") {
      includeGroup("androidx.media3")
      // the media3 API exposes ListenableFuture and ImmutableList
      includeDependency("com.google.guava:guava")
    }
    bundle("room") {
      includeGroup("androidx.room")
      includeGroup("androidx.sqlite")
    }
    bundle("serialization") {
      primary(libs.serialization.json)
      include("^org\\.jetbrains\\.kotlinx:kotlinx-serialization-.*")
    }
    bundle("okhttp") {
      includeDependency("com.squareup.okhttp3:okhttp")
      includeGroup("com.squareup.okio")
    }
    bundle("glance") {
      includeGroup("androidx.glance")
      includeGroup("androidx.collection")
    }
    bundle("materialKolor") {
      includeGroup("com.materialkolor")
    }
    bundle("paging") {
      includeGroup("androidx.paging")
    }
    bundle("composablePreviewScanner") {
      includeGroup("io.github.sergio-sastre.ComposablePreviewScanner")
    }
    bundle("mockk") {
      includeGroup("io.mockk")
    }
    bundle("robolectric") {
      includeGroup("org.robolectric")
    }
    bundle("androidxTest") {
      include("^androidx\\.test(\\..*)?:.*")
    }
    // modules whose API is built on a library, so their consumers get it through them
    bundle("dataStore") {
      includeDependency(":core:data:api")
      includeGroup("androidx.datastore")
    }
    bundle("navigation") {
      includeDependency(":navigation")
      includeGroup("androidx.navigation3")
    }
  }
}
