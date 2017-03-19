package de.ph1b.audiobook.features.chapterReader

/**
 * MetaData of a chapter
 *
 * @author Paul Woitaschek
 */
data class ChapterMetaData(var id3ID: String, var start: Long, var title: String? = null)