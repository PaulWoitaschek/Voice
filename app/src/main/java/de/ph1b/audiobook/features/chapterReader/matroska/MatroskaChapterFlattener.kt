package de.ph1b.audiobook.features.chapterReader.matroska

import android.util.SparseArray


object MatroskaChapterFlattener {

  private lateinit var target: SparseArray<String>
  private lateinit var preferredLanguages: List<String>

  @Synchronized
  fun toSparseArray(list: List<MatroskaChapter>, preferredLanguages: List<String>): SparseArray<String> {
    target = SparseArray()
    this.preferredLanguages = preferredLanguages
    addChapter(list, 0)
    return target
  }

  private fun addChapter(chapters: List<MatroskaChapter>, depth: Int) {
    chapters.forEachIndexed { i, chapter ->
      val duration = (chapter.startTime / 1000000).toInt() +
          if (i == 0) depth else 0
      // Simple hack with adding depth is needed because chapter
      // and it's first sub-chapter have usually the same starting time.
      val name = "+ ".repeat(depth) + (chapter.getName(preferredLanguages) ?: "Chapter ${i + 1}")
      target.put(duration, name)
      addChapter(chapter.children, depth + 1)
    }
  }
}
