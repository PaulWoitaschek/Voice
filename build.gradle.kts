import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import deps.Deps
import deps.Versions
import deps.configureBaseRepos

@Suppress("RemoveRedundantQualifierName")
buildscript {

  deps.configureBaseRepos(repositories)

  dependencies {
    classpath(deps.Deps.androidGradlePlugin)
    classpath(deps.Deps.Kotlin.gradlePlugin)
    classpath(deps.Deps.fabricGradlePlugin)
    classpath(deps.Deps.Kotlin.Serialization.gradlePlugin)
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.28.0"
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

allprojects {
  configureBaseRepos(repositories)

  configurations.all {
    resolutionStrategy {
      force(Deps.AndroidX.supportAnnotations)
      force(Deps.Kotlin.std)
      force("com.google.code.findbugs:jsr305:3.0.1")
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
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
      )
    }
  }
}

subprojects {
  plugins.whenPluginAdded {
    if (this is AppPlugin || this is LibraryPlugin) {
      convention.findByType(BaseExtension::class)?.let {
        it.dexOptions.preDexLibraries = System.getenv("CI") != "true"
      }
    }
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
      (tasks.findByName("testProprietaryReleaseUnitTest")
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
