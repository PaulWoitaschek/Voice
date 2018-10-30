package deps

import org.gradle.api.JavaVersion


object Versions {
  val sourceCompatibility = JavaVersion.VERSION_1_8
  val targetCompatibility = JavaVersion.VERSION_1_8

  const val compileSdk = 28
  const val minSdk = 23
  const val targetSdk = 27
  const val versionCode = 3060307
  const val versionName = "4.0.0-rc4"
}

object Deps {

  object AndroidX {
    const val supportAnnotations = "androidx.annotation:annotation:1.0.0"
    const val appCompat = "androidx.appcompat:appcompat:1.0.0"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:1.1.3"
    const val recyclerView = "androidx.recyclerview:recyclerview:1.0.0"
    const val transitions = "androidx.transition:transition:1.0.0"
    const val palette = "androidx.palette:palette:1.0.0"
    const val mediaCompat = "androidx.media:media:1.0.0"
    const val fragment = "androidx.fragment:fragment:1.0.0"
    const val ktx = "androidx.core:core-ktx:1.0.0"

    object Room {
      const val runtime = "androidx.room:room-runtime:2.0.0"
      const val compiler = "androidx.room:room-compiler:2.0.0"
      const val testing = "androidx.room:room-testing:2.0.0"
      const val rxJava = "androidx.room:room-rxjava2:2.0.0"
    }

    object Test {
      const val runner = "androidx.test:runner:1.1.0"
      const val junit = "androidx.test.ext:junit:1.0.0"
      const val core = "androidx.test:core:1.0.0"
    }
  }

  const val androidGradlePlugin = "com.android.tools.build:gradle:3.2.1"
  const val material = "com.google.android.material:material:1.0.0"
  const val floatingActionButton = "com.getbase:floatingactionbutton:1.10.1"
  const val materialDialogs = "com.afollestad.material-dialogs:core:0.9.6.0"
  const val materialCab = "com.afollestad:material-cab:1.3.0"
  const val picasso = "com.squareup.picasso:picasso:2.71828"
  const val tapTarget = "com.getkeepsafe.taptargetview:taptargetview:1.12.0"
  const val chapterReader = "com.github.PaulWoitaschek:ChapterReader:0.1.4"

  object Conductor {
    private const val version = "2.1.5"
    const val base = "com.bluelinelabs:conductor:$version"
    const val lifecycle = "com.bluelinelabs:conductor-rxlifecycle2:$version"
    const val support = "com.bluelinelabs:conductor-support:$version"
  }

  const val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.9.5@aar"
  const val fabricGradlePlugin = "io.fabric.tools:gradle:1.26.1"

  object Dagger {
    private const val version = "2.19"
    const val core = "com.google.dagger:dagger:$version"
    const val compiler = "com.google.dagger:dagger-compiler:$version"
  }

  object ExoPlayer {
    const val core = "com.google.android.exoplayer:exoplayer-core:2.9.0"
    private const val extensionVersion = "2.7.0-1"
    const val opus =
      "com.github.PaulWoitaschek.ExoPlayer-Extensions:opus:$extensionVersion"
    const val flac =
      "com.github.PaulWoitaschek.ExoPlayer-Extensions:flac:$extensionVersion"
  }

  const val moshi = "com.squareup.moshi:moshi:1.7.0"
  const val rxAndroid = "io.reactivex.rxjava2:rxandroid:2.1.0"
  const val rxJava = "io.reactivex.rxjava2:rxjava:2.2.3"
  const val rxPreferences = "com.f2prateek.rx.preferences2:rx-preferences:2.0.0"
  const val timber = "com.jakewharton.timber:timber:4.7.1"

  object Kotlin {
    private const val versionKotlin = "1.3.0"
    private const val versionCoroutines = "1.0.0"

    const val std = "org.jetbrains.kotlin:kotlin-stdlib:$versionKotlin"
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$versionCoroutines"
    const val coroutinesAndroid =
      "org.jetbrains.kotlinx:kotlinx-coroutines-android:$versionCoroutines"
    const val coroutinesRx = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$versionCoroutines"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$versionKotlin"
  }

  const val junit = "junit:junit:4.12"
  const val mockito = "org.mockito:mockito-core:2.23.0"
  const val mockitoKotlin = "com.nhaarman:mockito-kotlin-kt1.1:1.6.0"
  const val truth = "com.google.truth:truth:0.42"
  const val robolectric = "org.robolectric:robolectric:4.0"
  const val ktLint = "0.29.0"
}
