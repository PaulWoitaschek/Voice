import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.tasks.LintBaseTask
import com.android.builder.model.AndroidProject
import com.github.benmanes.gradle.versions.updates.DependencyUpdates
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import deps.Deps
import deps.Versions
import org.jlleitschuh.gradle.ktlint.KtlintExtension

buildscript {

  repositories {
    maven { setUrl("https://maven.google.com") }
    jcenter()
    google()
    maven { setUrl("https://maven.fabric.io/public") }
    mavenCentral()
  }

  dependencies {
    classpath(deps.Deps.androidGradlePlugin)
    classpath(deps.Deps.Kotlin.gradlePlugin)
    classpath(deps.Deps.fabricGradlePlugin)
  }
}

plugins {
  id("com.gradle.build-scan") version "1.16"
  id("com.github.ben-manes.versions") version "0.20.0"
  id("org.jlleitschuh.gradle.ktlint") version "6.2.0"
}

buildScan {
  setTermsOfServiceUrl("https://gradle.com/terms-of-service")
  setTermsOfServiceAgree("yes")
}

tasks {
  "dependencyUpdates"(DependencyUpdatesTask::class) {
    resolutionStrategy {
      componentSelection {
        all {
          val reject = listOf("rc", "beta", "alpha").any {
            candidate.version.contains(it, ignoreCase = true)
          }
          if (reject) {
            reject("blacklisted")
          }
          if (candidate.group == "javax.annotation" && candidate.version == "1.0-20050927.133100") {
            reject("blacklisted")
          }
        }
      }
    }
  }
}

allprojects {
  repositories {
    maven { setUrl("https://maven.google.com") }
    google()
    jcenter()
    mavenCentral()
    maven { setUrl("https://maven.fabric.io/public") }
    maven { setUrl("https://jitpack.io") }
  }

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
  }

  val artifactFolder = File(
    "${rootDir.absolutePath}/artifacts/${Versions.versionName}_${Versions.versionCode}"
  )

  val ciBuildApks = register<GradleBuild>("ciBuildApks") {
    val assembleTask = subprojects.single { it.name == "app" }
      .tasks.single { it.name == "assembleProprietaryRelease" }.also { task ->
      task.dependsOn.removeAll {
        val name = it.toString()
        name.contains("lint") || name.contains("crashlytics")
      }
    }
    dependsOn(assembleTask)
  }

  val copyCiApk = register<Copy>("copyCiApk") {
    from("app/build/outputs/apk/proprietary/release") {
      include("*.apk")
    }
    into(artifactFolder)
  }

  val allUnitTests = register<TestReport>("allUnitTests") {
    val tests = subprojects.mapNotNull { subProject ->
      (subProject.tasks.findByName("testProprietaryDebugUnitTest")
        ?: subProject.tasks.findByName("testDebugUnitTest")) as? Test
    }
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }

  register<GradleBuild>("ci") {
    val lint =
      subprojects.single { it.name == "app" }.tasks
        .single { it.name == "lintProprietaryDebug" } as LintBaseTask
    lint.lintOptions.htmlOutput = File(artifactFolder, "lint.html")
    dependsOn(
      "ktlintCheck",
      ciBuildApks,
      copyCiApk.get().mustRunAfter(ciBuildApks),
      allUnitTests,
      lint
    )
  }
}
