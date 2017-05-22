package de.ph1b.audiobook.features.bookSearch

/**
 * The parsed book search
 *
 * @author Paul Woitaschek
 */
data class BookSearch(
    val query: String?,
    val mediaFocus: String?,
    val album: String?,
    val artist: String?,
    val playList: String?
)