import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import deps.Deps
import deps.Versions
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import java.util.Properties

plugins {
  id("com.android.application")
  id("io.fabric")
  id("kotlin-android")
  id("kotlin-android-extensions")
  id("kotlin-kapt")
  id("com.github.triplet.play")
}

play {
  serviceAccountCredentials = File(rootDir, "play-publisher-key.json")
  track = "beta"
  defaultToAppBundles = true
  resolutionStrategy = "auto"
  outputProcessor = {
    versionNameOverride = "$versionNameOverride.$versionCode"
  }
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)

    versionCode = 1
    versionName = Versions.versionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val props = Properties()
      var propsFile = rootProject.file("signing.properties")
      if (!propsFile.canRead()) {
        println("Use CI keystore.")
        propsFile = rootProject.file(".circleci/signing/signing.properties")
      }
      props.load(propsFile.inputStream())
      storeFile = rootProject.file(props["STORE_FILE"]!!)
      storePassword = props["STORE_PASSWORD"] as String
      keyAlias = props["KEY_ALIAS"] as String
      keyPassword = props["KEY_PASSWORD"] as String
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      multiDexEnabled = false
    }
    getByName("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      ext["enableCrashlytics"] = false
      multiDexEnabled = true
    }
    all {
      signingConfig = signingConfigs.getByName("release")
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard.pro"))
    }
  }

  testOptions.unitTests.isReturnDefaultValues = true

  lintOptions {
    isCheckDependencies = true
    isIgnoreTestSources = true
  }

  compileOptions {
    setSourceCompatibility(Versions.sourceCompatibility)
    setTargetCompatibility(Versions.targetCompatibility)
  }

  //noinspection GroovyMissingReturnStatement
  packagingOptions {
    exclude("META-INF/rxjava.properties")
  }

  flavorDimensions("free")
  productFlavors {
    create("opensource") {
      dimension = "free"
    }
    create("proprietary") {
      dimension = "free"
    }
  }
  applicationVariants.all {
    outputs.all {
      this as BaseVariantOutputImpl
      outputFileName = "$versionCode-${parent!!.name}-$baseName-$versionName.apk"
    }
  }
}

androidExtensions {
  configure(delegateClosureOf<AndroidExtensionsExtension> {
    isExperimental = true
  })
}

dependencies {
  implementation(project(":core"))
  implementation(project(":common"))
  implementation(project(":data"))
  implementation(project(":covercolorextractor"))

  implementation(Deps.chapterReader)

  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.AndroidX.recyclerView)
  implementation(Deps.material)
  implementation(Deps.AndroidX.transitions)
  implementation(Deps.AndroidX.constraintLayout)
  implementation(Deps.AndroidX.mediaCompat)
  implementation(Deps.AndroidX.fragment)

  implementation(Deps.picasso)

  implementation(Deps.materialDialogs)
  implementation(Deps.materialCab)

  implementation(Deps.floatingActionButton)

  add("proprietaryImplementation", Deps.crashlytics) {
    isTransitive = true
  }

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  implementation(Deps.AndroidX.ktx)

  testImplementation(Deps.junit)
  testImplementation(Deps.truth)
  testImplementation(Deps.mockito)
  testImplementation(Deps.mockitoKotlin)

  androidTestImplementation(Deps.AndroidX.testRunner)
  androidTestImplementation(Deps.truth)

  implementation(Deps.rxJava)
  implementation(Deps.rxAndroid)
  implementation(Deps.rxPreferences)

  implementation(Deps.Kotlin.std)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)

  implementation(Deps.timber)

  implementation(Deps.ExoPlayer.core)
  implementation(Deps.ExoPlayer.flac)
  implementation(Deps.ExoPlayer.opus)

  implementation(Deps.Conductor.base)
  implementation(Deps.Conductor.support)
  implementation(Deps.Conductor.lifecycle)

  implementation(Deps.moshi)

  implementation(Deps.tapTarget)
}

tasks.create("fdroid").dependsOn(":app:assembleOpensourceRelease")

kotlin {
  experimental {
    coroutines = Coroutines.ENABLE
  }
}
