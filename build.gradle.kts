import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.builder.model.AndroidProject
import com.github.benmanes.gradle.versions.updates.DependencyUpdates
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import deps.Deps
import deps.Versions

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
}


buildScan {
  setTermsOfServiceUrl("https://gradle.com/terms-of-service")
  setTermsOfServiceAgree("yes")
}

tasks.register("appVersion") {
  print("#BEGIN_VERSION#${Versions.versionName}_${Versions.versionCode}#END_VERSION#")
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

apply(from = "predexDisabler.gradle")

tasks.register<Exec>("importStrings") {
  executable = "sh"
  args("-c", "tx pull -af --minimum-perc=5")
}
