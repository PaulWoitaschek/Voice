import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import deps.Deps
import deps.Versions
import deps.configureBaseRepos
import org.jlleitschuh.gradle.ktlint.KtlintExtension

@Suppress("RemoveRedundantQualifierName")
buildscript {

  deps.configureBaseRepos(repositories)

  dependencies {
    classpath(deps.Deps.androidGradlePlugin)
    classpath(deps.Deps.Kotlin.gradlePlugin)
    classpath(deps.Deps.fabricGradlePlugin)
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.26.0"
  id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

fun isNonStable(version: String): Boolean {
  return listOf("rc", "beta", "alpha").any {
    version.contains(it, ignoreCase = true)
  }
}

tasks.withType(DependencyUpdatesTask::class) {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
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
}

subprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  configure<KtlintExtension> {
    version.set(Deps.ktLint)
    android.set(true)
  }
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
      (subProject.tasks.findByName("testProprietaryDebugUnitTest")
          ?: subProject.tasks.findByName("testDebugUnitTest")) as? Test
    }
    val artifactFolder = File("${rootDir.absolutePath}/artifacts")
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }
}
