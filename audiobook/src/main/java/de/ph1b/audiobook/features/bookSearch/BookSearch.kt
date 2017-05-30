package de.ph1b.audiobook.features.bookSearch

/**
 * The parsed book search
 *
 * @author Paul Woitaschek
 */
data class BookSearch(
    val query: String? = null,
    val mediaFocus: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val playList: String? = null
)
