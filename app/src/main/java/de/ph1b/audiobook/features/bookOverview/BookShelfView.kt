package de.ph1b.audiobook.features.bookOverview

import java.util.UUID

interface BookShelfView {
  fun bookCoverChanged(bookId: UUID)
  fun render(state: BookShelfState)
}
