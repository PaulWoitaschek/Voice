package de.ph1b.audiobook.features.chapterReader.matroska

import android.util.SparseArray


object MatroskaChapterFlattener {

  fun toSparseArray(list: List<MatroskaChapter>, vararg preferredLanguages: String): SparseArray<String> {
    val res = SparseArray<String>()

    fun addChapter(chapters: List<MatroskaChapter>, depth: Int) {
      chapters.forEachIndexed { i, chapter ->
        res.put(
            // Simple hack with adding depth is needed because chapter
            // and it's first sub-chapter have usually the same starting time.
            (chapter.startTime / 1000000).toInt() + if (i == 0) depth else 0,
            "+ ".repeat(depth) + (chapter.getName(*preferredLanguages) ?: "Chapter ${i + 1}"))
        addChapter(chapter.children, depth + 1)
      }
    }

    addChapter(list, 0)

    return res
  }
}
