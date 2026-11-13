#!/usr/bin/env kotlin

import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

  fun toVersionString(): String = "$major.$minor.$patch"

  override fun compareTo(other: Version): Int {
    return compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
  }

  companion object {
    fun parse(version: String): Version? {
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
  check(exitCode == 0) {
    buildString {
      append("Command ${args.joinToString(" ")} failed with exit $exitCode")
      if (output.isNotBlank()) {
        append('\n')
        append(output)
      }
    }
  }
  return output
}

fun existingVersions(): List<Version> {
  // A draft gets its tag only when it's published, but its version name is already taken.
  val releaseTags = runCommand("gh", "release", "list", "--json", "tagName", "--jq", ".[].tagName").lines()
  return (runCommand("git", "tag").lines() + releaseTags)
    .mapNotNull(Version::parse)
}

// Codes count the minutes since the switch from codes derived from the version name, whose
// highest one on Play or on a tester's phone was 5411002. So each build gets a higher code than
// every build before it, without looking up which codes are taken.
val minuteCodesStart: Instant = Instant.parse("2026-11-13T20:00:00Z")
val firstMinuteCode = 5411003

// A clock that is far ahead would hand out codes that later builds can't go above.
val latestPlausibleBuild: Instant = Instant.parse("2040-01-01T00:00:00Z")

fun versionCode(now: Instant): Int {
  check(now >= minuteCodesStart) { "The clock says $now, before the version codes start at $minuteCodesStart." }
  check(now < latestPlausibleBuild) { "The clock says $now, which is after $latestPlausibleBuild. Is the clock wrong?" }
  return firstMinuteCode + Duration.between(minuteCodesStart, now).toMinutes().toInt()
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
  test(today = LocalDate.of(2025, 1, 1), versions = listOf(), expectedVersion = "25.1.1")
  test(today = LocalDate.of(2025, 1, 1), versions = listOf("25.1.5"), expectedVersion = "25.1.6")
  test(today = LocalDate.of(2025, 2, 5), versions = listOf("25.1.5"), expectedVersion = "25.2.1")
  test(today = LocalDate.of(2024, 12, 1), versions = listOf("24.11.2", "25.1.1"), expectedVersion = "24.12.1")

  fun testCode(now: String, expectedCode: Int) {
    val code = versionCode(Instant.parse(now))
    if (code != expectedCode) {
      throw IllegalStateException("Expected $expectedCode at $now but got $code")
    }
  }
  testCode(now = "2026-11-13T20:00:00Z", expectedCode = 5411003)
  testCode(now = "2026-11-13T20:00:59Z", expectedCode = 5411003)
  testCode(now = "2026-11-14T20:00:00Z", expectedCode = 5412443)
  testCode(now = "2039-12-31T23:59:00Z", expectedCode = 12317482)

  fun testRejected(now: String) {
    val result = runCatching { versionCode(Instant.parse(now)) }
    if (result.isSuccess) {
      throw IllegalStateException("Expected no code at $now but got ${result.getOrNull()}")
    }
  }
  testRejected("2026-11-13T19:59:59Z")
  testRejected("2040-01-01T00:00:00Z")
}

if ("--test" in args) {
  newVersionTests()
} else {
  val now = Instant.now()
  val versionCode = versionCode(now)
  val newVersion = newVersion(LocalDate.ofInstant(now, ZoneOffset.UTC), existingVersions())
  val tag = "${newVersion.toVersionString()}-$versionCode"
  val githubOutput = System.getenv("GITHUB_OUTPUT")

  println("Release tag: $tag")

  if (!githubOutput.isNullOrBlank()) {
    File(githubOutput).appendText(
      """
      |tag=$tag
      |version_name=${newVersion.toVersionString()}
      |version_code=$versionCode
      |
      """.trimMargin(),
    )
  }
}
