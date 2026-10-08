#!/usr/bin/env kotlin

// Lists strings that still need translating, plus translations whose placeholders don't match the English source.
// Usage, from the repository root: missing_translations.main.kts [locale ...], e.g. `de pt-rBR`. No locale means all.

import org.w3c.dom.Comment
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess

val stringsDir = File("core/strings/src/main/res")
require(stringsDir.isDirectory) { "Run this from the repository root." }

data class Resource(
  val name: String,
  // Plural items by quantity. A plain string has a single entry with an empty key.
  val texts: Map<String, String>,
  val comment: String?,
)

fun parse(file: File): Map<String, Resource> {
  val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
  val resources = linkedMapOf<String, Resource>()
  var comment: String? = null
  val nodes = document.documentElement.childNodes
  for (index in 0 until nodes.length) {
    when (val node = nodes.item(index)) {
      is Comment -> comment = node.data.trim()
      is Element -> {
        val texts = if (node.tagName == "plurals") {
          val items = node.getElementsByTagName("item")
          (0 until items.length).associate {
            val item = items.item(it) as Element
            item.getAttribute("quantity") to item.textContent
          }
        } else {
          mapOf("" to node.textContent)
        }
        val name = node.getAttribute("name")
        if (node.getAttribute("translatable") != "false") {
          resources[name] = Resource(name, texts, comment)
        }
        comment = null
      }
    }
  }
  return resources
}

val placeholderRegex = Regex("""%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])""")

// Normalizes `%s` and `%1$s` to the same form, so only the argument positions and types are compared.
fun placeholders(text: String): List<String> {
  var next = 1
  return placeholderRegex.findAll(text)
    .filter { it.groupValues[2] != "%" }
    .map { match ->
      val position = match.groupValues[1].ifEmpty { (next++).toString() }
      "%$position\$${match.groupValues[2]}"
    }
    .sorted()
    .toList()
}

fun Resource.sourceText(): String = texts[""] ?: texts.getValue("other")

fun Resource.display(): String = texts.entries.joinToString(" | ") { (quantity, text) ->
  if (quantity.isEmpty()) text else "$quantity=$text"
}

val source = parse(File(stringsDir, "values/strings.xml"))
val allLocales = stringsDir.listFiles().orEmpty()
  .filter { it.isDirectory && it.name.startsWith("values-") }
  .map { it.name.removePrefix("values-") }
  .sorted()
val requested = args.map { it.removePrefix("values-") }
val unknown = requested - allLocales.toSet()
require(unknown.isEmpty()) { "Unknown locales: $unknown. Known: $allLocales" }
val locales = requested.ifEmpty { allLocales }
val translations = locales.associateWith { parse(File(stringsDir, "values-$it/strings.xml")) }

val missing = source.values.mapNotNull { resource ->
  val missingIn = locales.filter { resource.name !in translations.getValue(it) }
  if (missingIn.isEmpty()) null else resource to missingIn
}
missing.forEach { (resource, missingIn) ->
  val where = if (missingIn.size == locales.size && locales.size > 1) "all" else missingIn.joinToString(" ")
  println("${resource.name} (missing in: $where)")
  println("  en: ${resource.display()}")
  resource.comment?.let { println("  note: $it") }
}

val problems = buildList {
  translations.forEach { (locale, resources) ->
    resources.values.forEach { translation ->
      val original = source[translation.name]
      if (original == null) {
        add("$locale ${translation.name}: not in values/strings.xml anymore, remove it")
        return@forEach
      }
      val expected = placeholders(original.sourceText())
      translation.texts.forEach { (quantity, text) ->
        val actual = placeholders(text)
        if (actual != expected) {
          val key = if (quantity.isEmpty()) translation.name else "${translation.name}[$quantity]"
          add("$locale $key: has $actual, English has $expected: $text")
        }
      }
    }
  }
}
if (problems.isNotEmpty()) {
  println()
  println("Problems:")
  problems.forEach { println("- $it") }
}

println()
println("Untranslated strings: ${missing.size}. Problems: ${problems.size}. Locales checked: ${locales.size}.")
if (problems.isNotEmpty()) exitProcess(1)
