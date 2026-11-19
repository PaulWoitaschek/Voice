package voice.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class LocaleConfigTest {

  private val repoRoot = File("..").canonicalFile
  private val stringsDir = File(repoRoot, "core/strings/src/main/res")
  private val localeConfigFile = File(repoRoot, "app/src/main/res/xml/locales_config.xml")
  private val storeListingDir = File(repoRoot, "fastlane/metadata/android")
  private val playProductsFile = File(repoRoot, "fastlane/play_products.json")
  private val documentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }

  @Test
  fun localeConfigListsEveryCompleteLocale() {
    val baseResources = resourceNames(File(stringsDir, "values/strings.xml"))
    val coverages =
      stringsDir
        .listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.name.startsWith("values-") }
        .map { directory ->
          val resources = resourceNames(File(directory, "strings.xml"))
          LocaleCoverage(
            locale = resourceLocaleToBcp47(directory.name.removePrefix("values-")),
            missing = baseResources - resources,
          )
        }
    val expected = coverages.filter { it.missing.isEmpty() }.map { it.locale }.toSet() + "en-US"
    val actual = configuredLocales()

    val problems = buildList {
      (expected - actual).sorted().forEach { add("$it is complete but not in the locale config") }
      (actual - expected).sorted().forEach { locale ->
        val missing = coverages.firstOrNull { it.locale == locale }?.missing
        add("$locale is in the locale config but misses ${missing?.sorted() ?: "its strings.xml"}")
      }
    }
    assertTrue(problems.isEmpty(), "Update $localeConfigFile:\n" + problems.joinToString("\n"))
  }

  @Test
  fun everyLocaleHasStoreListing() {
    val locales = configuredLocales()
    val storeListings =
      storeListingDir.listFiles().orEmpty().filter { it.isDirectory }.map { it.name }.sorted()
    val playProductListings =
      Json.parseToJsonElement(playProductsFile.readText())
        .jsonObject
        .getValue("listings")
        .jsonObject
        .keys
    val storeListingLimits =
      mapOf("title.txt" to 30, "short_description.txt" to 80, "full_description.txt" to 4000)

    val problems = buildList {
      // Google Play has no Sardinian store listing language.
      (locales - "sc").sorted().forEach { locale ->
        if (storeListings.none { storeListingMatches(it, locale) }) {
          add("$locale has no store listing")
        }
      }
      storeListings.forEach { listing ->
        if (locales.none { storeListingMatches(listing, it) }) {
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
          add("$listing has no listings in ${playProductsFile.name}")
        }
      }
      (playProductListings - storeListings.toSet()).sorted().forEach {
        add("$it in ${playProductsFile.name} has no store listing")
      }
    }
    assertTrue(problems.isEmpty(), problems.joinToString("\n"))
  }

  private fun resourceNames(file: File): Set<String> {
    if (!file.exists()) return emptySet()
    val resources =
      documentBuilderFactory.newDocumentBuilder().parse(file).documentElement.childNodes
    return buildSet {
      for (index in 0 until resources.length) {
        val attributes = resources.item(index).attributes ?: continue
        val name = attributes.getNamedItem("name")?.nodeValue ?: continue
        if (attributes.getNamedItem("translatable")?.nodeValue != "false") {
          add(name)
        }
      }
    }
  }

  private fun configuredLocales(): Set<String> {
    val locales =
      documentBuilderFactory
        .newDocumentBuilder()
        .parse(localeConfigFile)
        .getElementsByTagName("locale")
    return buildSet {
      for (index in 0 until locales.length) {
        val name =
          locales
            .item(index)
            .attributes
            .getNamedItemNS("http://schemas.android.com/apk/res/android", "name")
            ?.nodeValue
        if (!name.isNullOrBlank()) add(name)
      }
    }
  }

  private fun resourceLocaleToBcp47(resourceLocale: String): String {
    if (resourceLocale.startsWith("b+")) {
      return resourceLocale.removePrefix("b+").split("+").joinToString("-")
    }
    val parts = resourceLocale.split("-r", limit = 2)
    val language =
      when (parts[0]) {
        "in" -> "id"
        "iw" -> "he"
        "ji" -> "yi"
        else -> parts[0]
      }
    return if (parts.size == 1) language else "$language-${parts[1]}"
  }

  private fun storeListingMatches(listing: String, locale: String): Boolean {
    val listingParts = listing.split("-")
    val listingLanguage =
      when (listingParts[0]) {
        "iw" -> "he"
        "no" -> "nb"
        else -> listingParts[0]
      }
    val localeParts = locale.split("-")
    val localeRegion = localeParts.getOrNull(1)
    return listingLanguage == localeParts[0] &&
      (localeRegion == null || localeRegion == listingParts.getOrNull(1))
  }

  private data class LocaleCoverage(val locale: String, val missing: Set<String>)
}
