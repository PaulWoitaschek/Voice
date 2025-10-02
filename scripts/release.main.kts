#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import java.time.LocalDate

/**
 * Script to create a new release tag based on the date.
 */
class Release : CliktCommand() {

  private val runTests by option("--test").flag()
  private val noConfirm by option("--no-confirm").flag()

  data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

    fun toVersionString(): String = "$major.$minor.$patch"

    override fun compareTo(other: Version): Int {
      return compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
    }

    companion object {
      fun parse(version: String): Version? {
        // Accept tags like "1.2.3" or "v1.2.3" or "1.2.3-123456"
        val base = version.removePrefix("v").substringBefore("-")
        val split = base.split(".").mapNotNull(String::toIntOrNull)
        return if (split.size == 3) Version(split[0], split[1], split[2]) else null
      }
    }
  }

  fun runCommand(vararg args: String): String {
    val process = ProcessBuilder(*args)
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    val exitCode = process.waitFor()
    check(exitCode == 0) { "Command ${args.joinToString(" ")} failed with exit $exitCode" }
    return output
  }

  fun existingVersions(): List<Version> {
    return runCommand("git", "tag").lines()
      .mapNotNull(Version::parse)
  }

  fun Version.calculateVersionCode(): Int {
    val majorPart = major + 28
    val minorPart = "%02d".format(minor)
    val patchPart = "%03d".format(patch)
    return "$majorPart$minorPart$patchPart".toInt()
  }

  fun Version.calculateGitTag(): String {
    val versionName = toVersionString()
    val versionCode = this.calculateVersionCode()
    return "$versionName-$versionCode"
  }

  fun tagInGitAndPush(tag: String) {
    runCommand("git", "tag", "-a", tag, "-m", "Release $tag")
    runCommand("git", "push", "origin", "tag", tag)
  }

  fun newVersion(today: LocalDate, existingVersions: List<Version>): Version {
    val major = today.year - 2000
    val minor = today.monthValue
    val lastReleaseThisMonth = existingVersions
      .filter { it.major == major && it.minor == minor }
      .maxOfOrNull { it.patch }
    val patch = if (lastReleaseThisMonth == null) 1 else lastReleaseThisMonth + 1
    return Version(major = major, minor = minor, patch = patch)
  }

  fun newVersionTests() {
    fun test(
      today: LocalDate,
      versions: List<String>,
      expectedVersion: String,
    ) {
      val newVersion = newVersion(today, versions.map { Version.parse(it)!! })
      if (newVersion != Version.parse(expectedVersion)!!) {
        throw IllegalStateException("Expected $expectedVersion but got $newVersion")
      }
    }
    test(today = LocalDate.of(2025, 1, 1), versions = listOf(), expectedVersion = "8.1.0")
    test(today = LocalDate.of(2025, 1, 1), versions = listOf("8.1.5"), expectedVersion = "8.1.6")
    test(today = LocalDate.of(2025, 2, 5), versions = listOf("8.1.5"), expectedVersion = "8.2.0")
    test(today = LocalDate.of(2024, 12, 1), versions = listOf("7.11.0", "8.1.0"), expectedVersion = "7.12.0")
  }

  override fun run() {
    if (runTests) {
      echo("Running tests...")
      newVersionTests()
      echo("All tests passed!")
      return
    }

    echo("Calculating next version...")
    val existingVersions = existingVersions().sortedDescending()
    echo("Last 5 versions: ${existingVersions.take(5).joinToString { it.toVersionString() }}")
    val newVersion = newVersion(LocalDate.now(), existingVersions)
    val newTag = newVersion.calculateGitTag()
    val shouldRelease = confirmRelease(newTag)

    if (!shouldRelease) {
      echo("Aborting release")
      return
    }

    if (confirmPush(newTag)) {
      tagInGitAndPush(tag = newTag)
    }
  }

  private fun confirmRelease(newTag: String): Boolean {
    if (noConfirm) return true
    return YesNoPrompt(
      prompt = "Release version $newTag",
      terminal = terminal,
      default = true,
    ).ask() ?: false
  }

  private fun confirmPush(tag: String): Boolean {
    if (noConfirm) return true
    return YesNoPrompt(
      prompt = "Tag $tag and push to remote",
      terminal = terminal,
      default = true,
    ).ask() ?: false
  }
}

Release().main(args)
