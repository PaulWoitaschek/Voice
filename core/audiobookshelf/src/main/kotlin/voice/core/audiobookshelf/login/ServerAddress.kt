package voice.core.audiobookshelf.login

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Turns whatever was typed into the server urls to try, the most likely first. People paste the address of the
 * web app, sometimes with a page like `/login` at the end, or leave out the scheme. Servers at home are often only
 * reachable over http, so for them http goes first.
 */
internal fun serverUrlCandidates(input: String): List<String> {
  val trimmed = input.trim().trimEnd('/')
  if (trimmed.isEmpty()) return emptyList()

  val withSchemes = if ("://" in trimmed) {
    listOf(trimmed)
  } else {
    val host = trimmed.substringBefore('/').substringBeforeLast(':')
    if (isLocalHost(host)) {
      listOf("http://$trimmed", "https://$trimmed")
    } else {
      listOf("https://$trimmed", "http://$trimmed")
    }
  }

  return withSchemes
    .mapNotNull { it.toHttpUrlOrNull() }
    .flatMap { it.withParentPaths() }
    .map { it.toString() }
    .distinct()
}

private fun HttpUrl.withParentPaths(): List<HttpUrl> {
  val segments = pathSegments.filter { it.isNotEmpty() }
  return (segments.size downTo 0).map { count ->
    newBuilder()
      .encodedPath("/")
      .query(null)
      .fragment(null)
      .apply { segments.take(count).forEach { addPathSegment(it) } }
      .addPathSegment("")
      .build()
  }
}

private fun isLocalHost(host: String): Boolean {
  val lower = host.lowercase().removePrefix("[").removeSuffix("]")
  if (lower == "localhost" || '.' !in lower) return true
  if (listOf(".local", ".lan", ".home", ".internal", ".home.arpa").any { lower.endsWith(it) }) return true
  val octets = lower.split('.').mapNotNull { it.toIntOrNull() }
  if (octets.size != 4) return false
  return octets[0] == 10 ||
    octets[0] == 127 ||
    (octets[0] == 192 && octets[1] == 168) ||
    (octets[0] == 172 && octets[1] in 16..31) ||
    (octets[0] == 100 && octets[1] in 64..127)
}
