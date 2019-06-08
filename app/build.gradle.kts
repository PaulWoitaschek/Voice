import deps.Deps
import deps.Versions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
  id("com.android.application")
  id("io.fabric")
  id("kotlin-android")
  id("kotlin-android-extensions")
  id("kotlin-kapt")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)

    versionCode = Versions.versionCode
    versionName = Versions.versionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      setAbiFilters(
        listOf(
          "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        )
      )
    }
  }

  signingConfigs {
    create("release") {
      val props = Properties()
      var propsFile = File(rootDir, "signing/signing.properties")
      if (!propsFile.canRead()) {
        println("Use CI keystore.")
        propsFile = File(rootDir, "signing/ci/signing.properties")
      }
      props.load(propsFile.inputStream())
      storeFile = File(propsFile.parentFile, props["STORE_FILE"] as String)
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

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }

  lintOptions {
    isCheckDependencies = true
    isIgnoreTestSources = true
    isWarningsAsErrors = true
  }

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
  }

  packagingOptions {
    exclude("META-INF/rxjava.properties")
    exclude("META-INF/proguard/moshi.pro")
    pickFirst("META-INF/atomicfu.kotlin_module")
  }

  flavorDimensions("free")
  productFlavors {
    create("opensource") {
      setDimension("free")
    }
    create("proprietary") {
      setDimension("free")
    }
  }
}

androidExtensions {
  isExperimental = true
}

dependencies {
  implementation(project(":core"))
  implementation(project(":common"))
  implementation(project(":data"))
  implementation(project(":covercolorextractor"))
  implementation(project(":crashreporting"))

  implementation(Deps.chapterReader)

  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.AndroidX.recyclerView)
  implementation(Deps.material)
  implementation(Deps.AndroidX.transitions)
  implementation(Deps.AndroidX.constraintLayout)
  implementation(Deps.AndroidX.mediaCompat)
  implementation(Deps.AndroidX.fragment)

  implementation(Deps.picasso)

  implementation(Deps.MaterialDialog.core)
  implementation(Deps.MaterialDialog.input)
  implementation(Deps.materialCab)

  implementation(Deps.floatingActionButton)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  implementation(Deps.AndroidX.ktx)

  testImplementation(Deps.junit)
  testImplementation(Deps.truth)
  testImplementation(Deps.mockk)

  implementation(Deps.rxJava)
  implementation(Deps.rxAndroid)
  implementation(Deps.rxPreferences)

  implementation(Deps.Kotlin.std)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.Kotlin.coroutinesRx)

  implementation(Deps.timber)

  implementation(Deps.ExoPlayer.core)
  implementation(Deps.ExoPlayer.flac) { isTransitive = false }
  implementation(Deps.ExoPlayer.opus) { isTransitive = false }

  implementation(Deps.Conductor.base)
  implementation(Deps.Conductor.support)
  implementation(Deps.Conductor.lifecycle)

  implementation(Deps.moshi)

  implementation(Deps.tapTarget)
  testImplementation(Deps.AndroidX.Test.runner)
  testImplementation(Deps.AndroidX.Test.junit)
  testImplementation(Deps.AndroidX.Test.core)
  testImplementation(Deps.robolectric)

  androidTestImplementation(Deps.truth)
  androidTestImplementation(Deps.junit)
  androidTestImplementation(Deps.AndroidX.Test.runner)
  androidTestImplementation(Deps.AndroidX.Test.core)
  androidTestImplementation(Deps.AndroidX.Test.junit)
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = listOf(
      "-XXLanguage:+InlineClasses",
      "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
      "-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi",
      "-Xuse-experimental=kotlinx.coroutines.FlowPreview"
    )
  }
}

tasks.create("fdroid").dependsOn(":app:assembleOpensourceRelease")
