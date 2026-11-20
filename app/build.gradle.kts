@file:Suppress("UnstableApiUsage")

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Properties

plugins {
  id("voice.app")
  id("voice.compose")
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.baselineProfile)
}

val playGoogleServicesJson = layout.projectDirectory.file("src/play/google-services.json")

if (playGoogleServicesJson.asFile.canRead()) {
  pluginManager.apply(libs.plugins.googleServices.get().pluginId)
  pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
}

// Must match the minute codes in scripts/determine_release_tag.main.kts. It rounds up to the end
// of the UTC day, so the configuration cache lasts all day and the code stays above today's
// releases.
abstract class LocalVersionCode : ValueSource<Int, ValueSourceParameters.None> {
  override fun obtain(): Int {
    val endOfDay = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC)
    val minutes = Duration.between(Instant.parse("2026-11-13T20:00:00Z"), endOfDay).toMinutes()
    return 5411003 + minutes.toInt()
  }
}

val localVersionCode = providers.of(LocalVersionCode::class.java) {}

android {
  namespace = "voice.app"

  dependenciesInfo {
    // disable the dependencies info in apks to allow reproducible builds
    // see https://github.com/PaulWoitaschek/Voice/discussions/2862#discussioncomment-13622836
    includeInApk = false
  }

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    versionName = providers.gradleProperty("voice.versionName").orNull ?: "1.0.0"
    // F-Droid replaces the next line with sed, keep it on one line.
    versionCode =
      providers.gradleProperty("voice.versionCode").orNull?.toInt() ?: localVersionCode.get()
  }

  val distributionFlavor = "distribution"
  flavorDimensions += distributionFlavor
  productFlavors {
    register("free") {
      dimension = distributionFlavor
      buildConfigField(type = "Boolean", name = "INCLUDE_ANALYTICS", value = "false")
      pluginManager.withPlugin(libs.plugins.crashlytics.get().pluginId) {
        extensions.configure<CrashlyticsExtension>("firebaseCrashlytics") {
          mappingFileUploadEnabled = false
        }
      }
    }
    register("play") {
      dimension = distributionFlavor
      buildConfigField(type = "Boolean", name = "INCLUDE_ANALYTICS", value = "true")
    }
  }

  val signingDir = mainCheckoutDir(layout.projectDirectory.dir("..").asFile).resolve("signing")
  val signingPropertiesFile = signingDir.resolve("signing.properties")
  val signingKeystoreFile = signingDir.resolve("signing.keystore")
  val appSigningConfig =
    if (signingPropertiesFile.isFile) {
      val signingProperties =
        Properties().apply {
          signingPropertiesFile.inputStream().use(::load)
        }
      signingConfigs.create("signing") {
        storeFile = signingKeystoreFile
        storePassword = signingProperties.getProperty("STORE_PASSWORD")
        keyAlias = signingProperties.getProperty("KEY_ALIAS")
        keyPassword = signingProperties.getProperty("KEY_PASSWORD")
      }
    } else {
      null
    }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
    all {
      if (appSigningConfig != null) {
        signingConfig = appSigningConfig
      }
      setProguardFiles(
        listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard.pro",
        ),
      )
    }
  }

  lint {
    checkDependencies = true
    ignoreTestSources = true
    checkReleaseBuilds = false
    warningsAsErrors = providers.gradleProperty("voice.warningsAsErrors").get().toBooleanStrict()
  }

  packaging {
    with(resources.pickFirsts) {
      add("META-INF/atomicfu.kotlin_module")
      add("META-INF/core.kotlin_module")
    }
  }

  buildFeatures {
    buildConfig = true
  }
}

val validatePlayGoogleServices =
  tasks.register("validatePlayGoogleServices") {
    val playGoogleServicesJsonPath = playGoogleServicesJson.asFile.absolutePath

    doLast {
      check(File(playGoogleServicesJsonPath).canRead()) {
        "app/src/play/google-services.json is required for Play builds. " +
          "Use the free variant to build without Firebase."
      }
    }
  }

tasks
  .matching {
    it.name in
      setOf(
        "prePlayDebugBuild",
        "prePlayReleaseBuild",
        "prePlayNonMinifiedReleaseBuild",
        "prePlayBenchmarkReleaseBuild",
      )
  }
  .configureEach {
    dependsOn(validatePlayGoogleServices)
  }

tasks
  .matching {
    val isPlayTask = it.name.contains("Play")
    !isPlayTask &&
      (it.name.startsWith("process") && it.name.endsWith("GoogleServices") ||
        it.name.startsWith("injectCrashlytics"))
  }
  .configureEach {
    enabled = false
  }

// LocaleConfigTest reads these files from disk, so a change to them must rerun the tests.
tasks.withType<Test>().configureEach {
  inputs
    .files(
      file("../core/strings/src/main/res"),
      file("../fastlane/metadata/android"),
      file("../fastlane/play_products.json"),
    )
    .withPathSensitivity(PathSensitivity.RELATIVE)
    .withPropertyName("localeConfigTestFiles")
}

dependencies {
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.data.impl)
  implementation(projects.core.datastore)
  implementation(projects.core.playback.api)
  implementation(projects.core.playback.impl)
  implementation(projects.core.scanner)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.initializer)
  implementation(projects.features.playbackScreen)
  implementation(projects.navigation)
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.sleeptimer.impl)
  implementation(projects.features.settings)
  implementation(projects.features.folderPicker)
  implementation(projects.features.bookOverview)
  implementation(projects.core.search)
  implementation(projects.features.cover)
  implementation(projects.core.documentfile)
  implementation(projects.features.onboarding)
  implementation(projects.features.bookmark)
  implementation(projects.features.widget)
  implementation(projects.core.audiobookshelf.api)
  implementation(projects.core.audiobookshelf.impl)
  implementation(projects.core.foldersync)
  implementation(projects.features.audiobookshelf)

  implementation(libs.appCompat)
  implementation(libs.lifecycle.compose)
  implementation(libs.datastore)

  implementation(libs.navigation3.ui)

  implementation(libs.coil)

  implementation(libs.profileInstaller)

  add("playImplementation", libs.firebase.crashlytics)
  add("playImplementation", libs.firebase.analytics)
  add("playImplementation", projects.core.logging.crashlytics)
  add("playImplementation", projects.features.review.play)
  add("playImplementation", projects.features.support.play)
  add("freeImplementation", projects.features.review.noop)
  add("freeImplementation", projects.features.support.free)

  implementation(projects.core.remoteconfig.api)
  add("playImplementation", projects.core.remoteconfig.firebase)
  add("freeImplementation", projects.core.remoteconfig.noop)

  implementation(projects.core.analytics.api)
  add("playImplementation", projects.core.analytics.firebase)
  add("freeImplementation", projects.core.analytics.noop)

  debugImplementation(projects.core.logging.debug)

  implementation(libs.media3.exoplayer)
  implementation(libs.media3.session)

  testImplementation(kotlin("reflect"))
  testImplementation(libs.serialization.json)
}

// Git worktrees sign with the main checkout's keys, so the release key isn't copied into every
// worktree.
fun mainCheckoutDir(rootDir: File): File {
  val dotGit = rootDir.resolve(".git")
  if (!dotGit.isFile) return rootDir
  val gitDir = rootDir.resolve(dotGit.readText().removePrefix("gitdir:").trim())
  val commonDirFile = gitDir.resolve("commondir")
  if (!commonDirFile.isFile) return rootDir
  return gitDir.resolve(commonDirFile.readText().trim()).canonicalFile.parentFile
}

baselineProfile {
  // F-Droid builds from source, and the profile is not checked in.
  variants { register("play") { from(project(":baselineprofile")) } }
}
