#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt:clikt:2.6.0")

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class Release : CliktCommand() {

  override fun run() {
    execute("./gradlew", "app:bundleProprietaryRelease", "app:assembleProprietaryRelease")
    val appVersion = appVersion()
    val releaseFolder = File("releases", appVersion)
    releaseFolder.mkdirs()
    File("app/build/outputs/bundle/proprietaryRelease/app-proprietary-release.aab")
      .copyTo(File(releaseFolder, "app.aab"))
    File("app/build/outputs/apk/proprietary/release/app-proprietary-release.apk")
      .copyTo(File(releaseFolder, "app.apk"))
  }

  private fun appVersion(): String {
    return File("buildSrc/src/main/kotlin/deps/Deps.kt")
      .readLines()
      .mapNotNull {
        "versionName = (.*)".toRegex().find(it)?.groupValues?.getOrNull(1)
      }
      .single()
  }

  private fun execute(vararg command: String) {
    ProcessBuilder(*command)
      .inheritIO()
      .start()
      .waitFor()
      .also { check(it == 0) }
  }
}

Release().main(args)
