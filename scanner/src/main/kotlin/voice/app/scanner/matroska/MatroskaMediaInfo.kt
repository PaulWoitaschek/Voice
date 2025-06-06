package voice.app.scanner.matroska

import voice.data.MarkData

data class MatroskaMediaInfo(
    val album: String? = null,
    val artist: String? = null,
    val title: String? = null,
    val chapters: List<MarkData> = emptyList(),
)
