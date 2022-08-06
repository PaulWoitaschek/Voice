#!/usr/bin/env kotlin

import java.io.File

fun runCommand(vararg command: String) {
  println("runCommand=${command.toList()}")
  val process = ProcessBuilder(command.toList())
    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    .start()
  process.waitFor().also {
    check(it == 0) {
      "output=${process.errorStream.bufferedReader().readText()}"
    }
  }
}

// https://issuetracker.google.com/issues/193118030
runCommand("./gradlew", ":app:packageDebugAndroidTest")
runCommand(
  "./gradlew",
  ":app:screenshotDevicesGroupDebugAndroidTest",
  "-Pandroid.testInstrumentationRunnerArguments.class=voice.app.misc.ScreenshotCapture",
  "-Dorg.gradle.workers.max=1",
)

val deviceMapping = mapOf(
  "pixel5" to "phone",
  "nexus7" to "tablet",
  "nexus10" to "large-tablet",
).flatMap { (device, qualifier) ->
  val deviceOutput = File("app/build/outputs/managed_device_android_test_additional_output", device)
  listOf(
    "book_overview_grid",
    "book_overview_list",
    "settings",
    "folder_picker",
  ).mapIndexed { index, screenshotName ->
    val sourceFile = File(deviceOutput, "$screenshotName.png")
    val qualifierFolder = File("app/src/main/play/listings/en-US/graphics", "$qualifier-screenshots")
    val targetFile = File(qualifierFolder, "${index + 1}.png")
    sourceFile to targetFile
  }
}
  .forEach { (sourceFile, targetFile) ->
    println("copy $sourceFile to $targetFile")
    sourceFile.copyTo(targetFile, overwrite = true)
  }
