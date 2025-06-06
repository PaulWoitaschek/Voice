package voice.app.scanner.matroska

data class MatroskaMediaInfo(
    val album: String? = null,
    val artist: String? = null,
    val title: String? = null,
    val chapters: List<Chapter> = emptyList(),
)
