import deps.Versions
import deps.configureBaseRepos

@Suppress("RemoveRedundantQualifierName")
buildscript {

  deps.configureBaseRepos(repositories)

  dependencies {
    classpath(deps.Deps.androidGradlePlugin)
    classpath(deps.Deps.Kotlin.gradlePlugin)
    classpath(deps.Deps.Kotlin.Serialization.gradlePlugin)
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.38.0"
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

allprojects {
  configureBaseRepos(repositories)

  plugins.withType(com.android.build.gradle.internal.plugins.BasePlugin::class.java) {
    with(extension) {
      defaultConfig {
        multiDexEnabled = true
        minSdkVersion(24)
        targetSdkVersion(30)
      }
      compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
      }
      compileSdkVersion(30)

      dependencies {
        add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:1.1.1")
      }
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = listOf(
        "-Xinline-classes",
        "-progressive",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlinx.coroutines.FlowPreview",
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts"
      )
    }
  }
}

subprojects {
  fun addCoreDependencies() {
    if (path != ":core") {
      dependencies.add("implementation", project(":core"))
    }
  }
  plugins.withId("kotlin") {
    addCoreDependencies()
  }
  plugins.withId("kotlin-android") {
    addCoreDependencies()
  }
}

tasks {
  register<Exec>("importStrings") {
    executable = "sh"
    args("-c", "tx pull -af --minimum-perc=5")
    finalizedBy(":core:lintDebug")
  }

  register("appVersion") {
    doLast {
      print("#BEGIN_VERSION#${Versions.versionName}#END_VERSION#")
    }
  }

  register<TestReport>("allUnitTests") {
    val tests = subprojects.mapNotNull { subProject ->
      val tasks = subProject.tasks
      (
        tasks.findByName("testProprietaryReleaseUnitTest")
          ?: tasks.findByName("testReleaseUnitTest")
          ?: tasks.findByName("test")
        ) as? Test
    }
    val artifactFolder = File("${rootDir.absolutePath}/artifacts")
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }
}

apply(from = "dependency_updates.gradle")
apply(from = "apply_ktlint.gradle")
