/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.persistence.internals

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.Chapter
import org.jetbrains.anko.db.*
import java.io.File
import javax.inject.Inject


/**
 * Provides access to the peristent storage for bookmarks.
 *
 * @author: Paul Woitaschek
 */
class InternalBookRegister
@Inject constructor(internalDb: InternalDb) {

    private val db by lazy { internalDb.writableDatabase }

    private fun books(active: Boolean) = db.select(BookTable.TABLE_NAME)
            .whereSimple("${BookTable.ACTIVE} =?", if (active) "1" else "0")
            .columns(BookTable.ID,
                    BookTable.NAME,
                    BookTable.AUTHOR,
                    BookTable.CURRENT_MEDIA_PATH,
                    BookTable.PLAYBACK_SPEED,
                    BookTable.ROOT,
                    BookTable.TIME,
                    BookTable.TYPE,
                    BookTable.USE_COVER_REPLACEMENT)
            .parseList(rowParser { bookId: Long,
                                   bookName: String,
                                   bookAuthor: String?,
                                   currentPath: String,
                                   bookSpeed: Float,
                                   bookRoot: String,
                                   bookTime: Int,
                                   bookType: String,
                                   bookUseCoverReplacement: Int ->
                val chapters = db.select(ChapterTable.TABLE_NAME)
                        .columns(ChapterTable.NAME, ChapterTable.DURATION, ChapterTable.PATH)
                        .whereSimple("${ChapterTable.BOOK_ID} =?", bookId.toString())
                        .parseList(rowParser { name: String, duration: Int, path: String ->
                            Chapter(File(path), name, duration)
                        })

                return@rowParser Book(bookId,
                        Book.Type.valueOf(bookType),
                        bookUseCoverReplacement == 1,
                        bookAuthor,
                        File(currentPath),
                        bookTime,
                        bookName,
                        chapters,
                        bookSpeed,
                        bookRoot)
            })

    fun activeBooks() = books(true)

    fun orphanedBooks() = books(false)

    private fun setBookVisible(bookId: Long, visible: Boolean) = db.update(BookTable.TABLE_NAME, BookTable.ACTIVE to if (visible) 1 else 0)
            .whereSimple("${BookTable.ID} =?", bookId.toString())
            .exec()


    fun revealBook(bookId: Long) {
        setBookVisible(bookId, true)
    }

    fun hideBook(bookId: Long) {
        setBookVisible(bookId, false)
    }

    private fun SQLiteDatabase.insert(chapter: Chapter, bookId: Long) = insert(ChapterTable.TABLE_NAME,
            ChapterTable.DURATION to chapter.duration,
            ChapterTable.NAME to chapter.name,
            ChapterTable.PATH to chapter.file.absolutePath,
            ChapterTable.BOOK_ID to bookId)

    private fun Book.getContentValues() = ContentValues().apply {
        put(BookTable.NAME, name)
        put(BookTable.AUTHOR, author)
        put(BookTable.ACTIVE, 1)
        put(BookTable.CURRENT_MEDIA_PATH, currentFile.absolutePath)
        put(BookTable.PLAYBACK_SPEED, playbackSpeed)
        put(BookTable.ROOT, root)
        put(BookTable.TIME, time)
        put(BookTable.TYPE, type.name)
        put(BookTable.USE_COVER_REPLACEMENT, useCoverReplacement)
    }

    fun updateBook(book: Book) = db.transaction {
        // update book itself
        val bookCv = book.getContentValues()
        update(BookTable.TABLE_NAME, bookCv, "${BookTable.ID}=?", arrayOf(book.id.toString()))

        // delete old chapters and replace them with new ones
        delete(ChapterTable.TABLE_NAME, "${BookTable.ID}=?", arrayOf(book.id.toString()))
        book.chapters.forEach { insert(it, book.id) }
    }

    fun addBook(toAdd: Book) = db.asTransaction {
        val bookCv = toAdd.getContentValues()
        val bookId = insert(BookTable.TABLE_NAME, null, bookCv)
        val newBook = toAdd.copy(id = bookId)
        newBook.chapters.forEach { insert(it, bookId) }
        return@asTransaction newBook
    }
}