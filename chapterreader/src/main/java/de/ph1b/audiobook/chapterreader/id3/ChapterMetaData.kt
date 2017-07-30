package de.ph1b.audiobook.chapterreader.id3

internal data class ChapterMetaData(
    var id3ID: String,
    var start: Int,
    var title: String? = null
)
