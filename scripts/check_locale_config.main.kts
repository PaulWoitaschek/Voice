#!/usr/bin/env kotlin
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.12.0")

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

val repoRoot = File(".").canonicalFile
val stringsDir = File(repoRoot, "core/strings/src/main/res")
val localeConfigFile = File(repoRoot, "app/src/main/res/xml/locales_config.xml")
val minCoverage = 100
val baseLocales = setOf("en-US")
val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
  isNamespaceAware = true
}

fun resourceNames(file: File): Set<String> {
  val document = documentBuilderFactory.newDocumentBuilder().parse(file)
  val resources = document.documentElement.childNodes
  val names = mutableSetOf<String>()
  for (index in 0 until resources.length) {
    val node = resources.item(index)
    val name = node.attributes?.getNamedItem("name")?.nodeValue
    val translatable = node.attributes?.getNamedItem("translatable")?.nodeValue != "false"
    if (name != null && translatable) {
      names += name
    }
  }
  return names
}

fun resourceLocaleToBcp47(resourceLocale: String): String {
  if (resourceLocale.startsWith("b+")) {
    return resourceLocale.removePrefix("b+").split("+").joinToString("-")
  }

  val parts = resourceLocale.split("-r", limit = 2)
  val language = when (parts[0]) {
    "in" -> "id"
    "iw" -> "he"
    "ji" -> "yi"
    else -> parts[0]
  }
  return if (parts.size == 1) language else "$language-${parts[1]}"
}

fun configuredLocales(): Set<String> {
  val document = documentBuilderFactory.newDocumentBuilder().parse(localeConfigFile)
  val locales = document.getElementsByTagName("locale")
  val namespace = "http://schemas.android.com/apk/res/android"
  return buildSet {
    for (index in 0 until locales.length) {
      val locale = locales.item(index)
      val name = locale.attributes.getNamedItemNS(namespace, "name")?.nodeValue
      if (!name.isNullOrBlank()) {
        add(name)
      }
    }
  }
}

data class LocaleCoverage(
  val locale: String,
  val present: Int,
  val total: Int,
) {
  val percentage: Double = present.toDouble() / total * 100
}

val baseResources = resourceNames(File(stringsDir, "values/strings.xml"))
val localizedCoverages = stringsDir.listFiles()
  .orEmpty()
  .filter { it.isDirectory && it.name.startsWith("values-") }
  .map { directory ->
    val resources = resourceNames(File(directory, "strings.xml"))
    LocaleCoverage(
      locale = resourceLocaleToBcp47(directory.name.removePrefix("values-")),
      present = (resources intersect baseResources).size,
      total = baseResources.size,
    )
  }
  .sortedBy { it.locale }

val expectedLocales = baseLocales + localizedCoverages
  .filter { it.percentage >= minCoverage }
  .map { it.locale }

val actualLocales = configuredLocales()
val missing = expectedLocales - actualLocales
val unexpected = actualLocales - expectedLocales

if (missing.isNotEmpty() || unexpected.isNotEmpty()) {
  println("Locale config does not match localization coverage.")
  println("Expected locales (>= ${minCoverage}% coverage plus ${baseLocales.sorted().joinToString()}):")
  println(expectedLocales.sorted().joinToString(", "))
  println("Actual locales:")
  println(actualLocales.sorted().joinToString(", "))

  if (missing.isNotEmpty()) {
    println("Missing from locale config:")
    missing.sorted().forEach { println("- $it") }
  }

  if (unexpected.isNotEmpty()) {
    println("Locales in config at or below ${minCoverage}% coverage:")
    unexpected.sorted().forEach { println("- $it") }
  }

  println("Coverage:")
  localizedCoverages.sortedByDescending { it.percentage }.forEach {
    println("- ${it.locale}: ${it.present}/${it.total} (${String.format("%.1f", it.percentage)}%)")
  }

  throw IllegalStateException("Update $localeConfigFile to match localization coverage.")
}

fun storeListingMatches(listing: String, locale: String): Boolean {
  val listingParts = listing.split("-")
  val listingLanguage = when (listingParts[0]) {
    "iw" -> "he"
    "no" -> "nb"
    else -> listingParts[0]
  }
  val localeParts = locale.split("-")
  val localeRegion = localeParts.getOrNull(1)
  return listingLanguage == localeParts[0] && (localeRegion == null || localeRegion == listingParts.getOrNull(1))
}

val storeListingDir = File(repoRoot, "fastlane/metadata/android")

// Google Play has no Sardinian store listing language.
val localesWithoutStoreListing = setOf("sc")
val storeListingLimits = mapOf(
  "title.txt" to 30,
  "short_description.txt" to 80,
  "full_description.txt" to 4000,
)
val storeListings = storeListingDir.listFiles()
  .orEmpty()
  .filter { it.isDirectory }
  .map { it.name }
  .sorted()
val playProductsPath = "fastlane/play_products.json"
val playProductListings = Json.parseToJsonElement(File(repoRoot, playProductsPath).readText())
  .jsonObject
  .getValue("listings")
  .jsonObject
  .keys
val storeListingProblems = buildList {
  (actualLocales - localesWithoutStoreListing).sorted().forEach { locale ->
    if (storeListings.none { storeListingMatches(it, locale) }) {
      add("$locale has no store listing")
    }
  }
  storeListings.forEach { listing ->
    if (actualLocales.none { storeListingMatches(listing, it) }) {
      add("$listing is not in the locale config")
    }
    storeListingLimits.forEach { (fileName, limit) ->
      val file = File(storeListingDir, "$listing/$fileName")
      val length = if (file.exists()) file.readText().trim().length else 0
      if (length == 0 || length > limit) {
        add("$listing/$fileName has $length characters, needs 1 to $limit")
      }
    }
    if (listing !in playProductListings) {
      add("$listing has no listings in $playProductsPath")
    }
  }
  (playProductListings - storeListings.toSet()).sorted().forEach { language ->
    add("$language in $playProductsPath has no store listing")
  }
}

if (storeListingProblems.isNotEmpty()) {
  println("Play Store listings do not match the locale config:")
  storeListingProblems.forEach { println("- $it") }
  throw IllegalStateException("Add a store listing and Play product listings for every locale in $localeConfigFile.")
}

println("Every locale has a store listing and Play product listings.")
println("Locale config matches localization coverage.")
localizedCoverages.sortedByDescending { it.percentage }.forEach {
  val status = if (it.percentage >= minCoverage) "included" else "excluded"
  println("- ${it.locale}: ${it.present}/${it.total} (${String.format("%.1f", it.percentage)}%) $status")
}
