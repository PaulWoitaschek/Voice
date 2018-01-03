@file:Suppress("unused")

package de.ph1b.audiobook.versioning

object Versions {

  val buildTools = "26.0.2"
  val compileSdk = 26
  val minSdk = 21
  val targetSdk = 26

  private val major = 3
  private val minor = 6
  private val patch = 3
  private val build = 0
  private val type = ""
  val versionName = versionName(major, minor, patch, build, type)
  val versionCode = versionCode(major, minor, patch, build)

  private fun versionCode(major: Int, minor: Int, patch: Int, build: Int) =
      1_000_000 * major + 10_000 * minor + 100 * patch + build

  private fun versionName(major: Int, minor: Int, patch: Int, build: Int, type: String): String {
    var versionName = "$major.$minor.$patch"
    val appendBuild = build != 0
    if (appendBuild || !type.isEmpty()) {
      versionName += "-$type"
      if (appendBuild) {
        versionName += build
      }
    }
    return versionName
  }
}

object Dependencies {

  object Support {
    private val version = "27.0.2"
    val supportAnnotations = "com.android.support:support-annotations:$version"
    val appCompat = "com.android.support:appcompat-v7:$version"
    val constraintLayout = "com.android.support.constraint:constraint-layout:1.1.0-beta4"
    val design = "com.android.support:design:$version"
    val recyclerView = "com.android.support:recyclerview-v7:$version"
    val transitions = "com.android.support:transition:$version"
    val v13 = "com.android.support:support-v13:$version"
    val palette = "com.android.support:palette-v7:$version"
    val testRunner = "com.android.support.test:runner:1.0.1"
  }

  private val androidPlugin = "3.0.1"
  val dataBindingCompiler = "com.android.databinding:compiler:$androidPlugin"
  val androidGradlePlugin = "com.android.tools.build:gradle:$androidPlugin"
  val floatingActionButton = "com.getbase:floatingactionbutton:1.10.1"
  val materialDialogs = "com.afollestad.material-dialogs:core:0.9.6.0"
  val materialCab = "com.afollestad:material-cab:0.1.12"
  val picasso = "com.squareup.picasso:picasso:2.5.2"
  val tapTarget = "com.getkeepsafe.taptargetview:taptargetview:1.9.1"
  val chapterReader = "com.github.PaulWoitaschek:ChapterReader:0.1.3"

  object Conductor {
    private val version = "2.1.4"
    val base = "com.bluelinelabs:conductor:$version"
    val lifecycle = "com.bluelinelabs:conductor-rxlifecycle2:$version"
    val support = "com.bluelinelabs:conductor-support:$version"
  }

  val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.8.0@aar"
  val fabricGradlePlugin = "io.fabric.tools:gradle:1.25.1"

  object Dagger {
    private val version = "2.14.1"
    val core = "com.google.dagger:dagger:$version"
    val android = "com.google.dagger:dagger-android-support:$version"
    val androidProcessor = "com.google.dagger:dagger-android-processor:$version"
    val compiler = "com.google.dagger:dagger-compiler:$version"
  }

  object ExoPlayer {
    private val version = "2.6.0"
    val core = "com.google.android.exoplayer:exoplayer-core:$version"
    val opus = "com.github.PaulWoitaschek.ExoPlayer-Extensions:opus:$version"
    val flac = "com.github.PaulWoitaschek.ExoPlayer-Extensions:flac:$version"
  }

  val moshi = "com.squareup.moshi:moshi:1.5.0"
  val rxAndroid = "io.reactivex.rxjava2:rxandroid:2.0.1"
  val rxInterop = "com.github.akarnokd:rxjava2-interop:0.11.2"
  val rxJava = "io.reactivex.rxjava2:rxjava:2.1.8"
  val rxPreferences = "com.f2prateek.rx.preferences:rx-preferences:1.0.2"
  val timber = "com.jakewharton.timber:timber:4.6.0"

  object Kotlin {
    private val coroutineVersion = "0.21"
    private val kotlinVersion = "1.2.10"
    val std = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion"
    val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion"
    val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
  }

  val junit = "junit:junit:4.12"
  val mockito = "org.mockito:mockito-core:2.13.0"
  val mockitoKotlin = "com.nhaarman:mockito-kotlin-kt1.1:1.5.0"
  val truth = "com.google.truth:truth:0.37"
}
