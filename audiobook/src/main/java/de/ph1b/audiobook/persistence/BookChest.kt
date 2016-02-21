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

package de.ph1b.audiobook.persistence

import Slimber
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.ph1b.audiobook.assertMain
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.Bookmark
import de.ph1b.audiobook.model.Chapter
import rx.Observable
import rx.Subscriber
import rx.subjects.PublishSubject
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * This is the helper for the apps database.

 * @author Paul Woitaschek
 */
@Singleton
class BookChest
@Inject
constructor(internalDb: InternalDb) {
    private val active: MutableList<Book> by lazy {
        synchronized(this) {
            val cursor = db.rawQuery("$FULL_PROJECTION $APPEND_WHERE_ACTIVE", arrayOf(BOOLEAN_TRUE.toString()))
            val active = ArrayList<Book>(cursor.count)
            cursor.moveToNextLoop {
                val book = byProjection(this)
                active.add(book)
            }
            active
        }
    }
    private val APPEND_WHERE_ACTIVE = " WHERE bt.${BookTable.ACTIVE} =?"
    private val orphaned: MutableList<Book> by lazy {
        synchronized(this) {
            val cursor = db.rawQuery("$FULL_PROJECTION $APPEND_WHERE_ACTIVE", arrayOf(BOOLEAN_FALSE.toString()))
            val active = ArrayList<Book>(cursor.count)
            cursor.moveToNextLoop {
                val book = byProjection(cursor)
                active.add(book)
            }
            active
        }
    }

    inline fun <T> Subscriber<T>.onNextIfSubscribed(function: () -> T) {
        if (!isUnsubscribed) onNext(function.invoke())
    }

    fun <T> Subscriber<T>.onCompletedIfSubscribed() {
        if (!isUnsubscribed) onCompleted()
    }

    fun deleteBookmark(id: Long) {
        db.delete(BookmarkTable.TABLE_NAME, "${BookmarkTable.ID} =?", arrayOf(id.toString()))
    }

    inline fun <T> Iterable<T>.mapIf(condition: (T) -> Boolean, transform: (T) -> T): List<T> {
        val array = ArrayList<T>()
        for (t in this) {
            array.add(if (condition(t)) transform(t) else t)
        }
        return array
    }


    fun addBookmark(bookmark: Bookmark): Bookmark {
        val mutalbeList = ArrayList<String>()
        val new = mutalbeList.map { it.toInt() }


        val cv = bookmark.toContentValues()
        val insertedId = db.insertOrThrow(BookmarkTable.TABLE_NAME, null, cv)
        return bookmark.copy(id = insertedId)
    }

    fun addBookmarkAtBookPosition(book: Book, title: String) {
        val addedBookmark = Bookmark(book.currentChapter().file, title, book.time)
        addBookmark(addedBookmark)
        Slimber.v { "Added bookmark=$addedBookmark" }
    }

    fun bookmarks(book: Book) = Observable.just(db)
            .map {
                val whereBuilder = StringBuilder()
                val pathAlike = "${BookmarkTable.PATH} =?"
                for (i in 0..book.chapters.size - 1) {
                    if (i > 0) whereBuilder.append(" OR ")
                    whereBuilder.append(pathAlike)
                }
                it.query(BookmarkTable.TABLE_NAME,
                        arrayOf(BookmarkTable.PATH, BookmarkTable.TIME, BookmarkTable.TITLE, BookmarkTable.ID),
                        whereBuilder.toString(),
                        book.chapters.map { it.file.absolutePath }.toTypedArray(),
                        null, null, null)
            }
            .flatMap { cursor ->
                Observable.create<Bookmark> {
                    cursor.moveToNextLoop {
                        it.onNextIfSubscribed {
                            cursor.toBookmark()
                        }
                    }
                    it.onCompletedIfSubscribed()
                }
            }

    private val db: SQLiteDatabase by lazy {
        synchronized(this) {
            internalDb.writableDatabase
        }
    }
    private val added = PublishSubject.create<Book>()
    private val removed = PublishSubject.create<Book>()
    private val updated = PublishSubject.create<Book>()

    private val BOOLEAN_TRUE = 1
    private val BOOLEAN_FALSE = 0
    private val KEY_CHAPTER_DURATIONS = "chapterDurations"
    private val KEY_CHAPTER_NAMES = "chapterNames"
    private val KEY_CHAPTER_PATHS = "chapterPaths"
    private val stringSeparator = "-~_"
    private val FULL_PROJECTION = "SELECT" +
            " bt." + BookTable.ID +
            ", bt." + BookTable.NAME +
            ", bt." + BookTable.AUTHOR +
            ", bt." + BookTable.CURRENT_MEDIA_PATH +
            ", bt." + BookTable.PLAYBACK_SPEED +
            ", bt." + BookTable.ROOT +
            ", bt." + BookTable.TIME +
            ", bt." + BookTable.TYPE +
            ", bt." + BookTable.USE_COVER_REPLACEMENT +
            ", bt." + BookTable.ACTIVE +
            ", ct." + KEY_CHAPTER_PATHS +
            ", ct." + KEY_CHAPTER_NAMES +
            ", ct." + KEY_CHAPTER_DURATIONS +
            " FROM " +
            BookTable.TABLE_NAME + " AS bt " +
            " left join" +
            "   (select " + ChapterTable.BOOK_ID + "," +
            "           group_concat(" + ChapterTable.PATH + ", '" + stringSeparator + "') as " + KEY_CHAPTER_PATHS + "," +
            "           group_concat(" + ChapterTable.DURATION + ") as " + KEY_CHAPTER_DURATIONS + "," +
            "           group_concat(" + ChapterTable.NAME + ", '" + stringSeparator + "') as " + KEY_CHAPTER_NAMES +
            "    from " + ChapterTable.TABLE_NAME +
            "    group by " + ChapterTable.BOOK_ID + ") AS ct on ct." + ChapterTable.BOOK_ID + " = bt." + BookTable.ID

    @Synchronized fun removedObservable(): Observable<Book> = removed.asObservable()

    @Synchronized fun addedObservable(): Observable<Book> = added.asObservable()

    @Synchronized fun updateObservable(): Observable<Book> = updated.asObservable()

    @Synchronized fun addBook(book: Book) {
        var newBook = book
        Slimber.v { "addBook=${newBook.name}" }
        assertMain()

        db.asTransaction {
            val bookCv = BookTable.getContentValues(newBook)
            val bookId = insert(BookTable.TABLE_NAME, null, bookCv)

            newBook = newBook.copy(id = bookId)

            for (c in newBook.chapters) {
                val chapterCv = ChapterTable.getContentValues(c, newBook.id)
                insert(ChapterTable.TABLE_NAME, null, chapterCv)
            }
        }

        active.add(newBook)
        added.onNext(newBook)
    }

    /**
     * All active books. We
     */
    val activeBooks = Observable.fromCallable { synchronized(this) { ArrayList(active) } }
            .flatMapIterable { list -> list }

    @Synchronized fun getOrphanedBooks(): List<Book> {
        return ArrayList(orphaned)
    }

    @Synchronized fun updateBook(book: Book) {
        Slimber.v { "updateBook=${book.name} with time ${book.time}" }
        assertMain()

        val bookIterator = active.listIterator()
        while (bookIterator.hasNext()) {
            val next = bookIterator.next()
            if (book.id == next.id) {
                bookIterator.set(book)

                db.asTransaction {
                    // update book itself
                    val bookCv = BookTable.getContentValues(book)
                    update(BookTable.TABLE_NAME, bookCv, "${BookTable.ID}=?", arrayOf(book.id.toString()))

                    // delete old chapters and replace them with new ones
                    delete(ChapterTable.TABLE_NAME, "${BookTable.ID}=?", arrayOf(book.id.toString()))
                    for (c in book.chapters) {
                        val chapterCv = ChapterTable.getContentValues(c, book.id)
                        insert(ChapterTable.TABLE_NAME, null, chapterCv)
                    }
                }

                break
            }
        }

        updated.onNext(book)
    }

    @Synchronized fun hideBook(book: Book) {
        Slimber.v { "hideBook=${book.name}" }
        assertMain()

        val iterator = active.listIterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.id == book.id) {
                iterator.remove()
                val cv = ContentValues()
                cv.put(BookTable.ACTIVE, BOOLEAN_FALSE)
                db.update(BookTable.TABLE_NAME, cv, "${BookTable.ID}=?", arrayOf(book.id.toString()))
                break
            }
        }
        orphaned.add(book)
        removed.onNext(book)
    }

    @Synchronized fun revealBook(book: Book) {
        assertMain()

        val orphanedBookIterator = orphaned.iterator()
        while (orphanedBookIterator.hasNext()) {
            if (orphanedBookIterator.next().id == book.id) {
                orphanedBookIterator.remove()
                val cv = ContentValues()
                cv.put(BookTable.ACTIVE, BOOLEAN_TRUE)
                db.update(BookTable.TABLE_NAME, cv, "${BookTable.ID}=?", arrayOf(book.id.toString()))
                break
            }
        }
        active.add(book)
        added.onNext(book)
    }

    private fun generateChapters(position: IntArray, path: Array<String>, title: Array<String>): List<Chapter> {
        check(position.size == path.size && path.size == title.size,
                { "Positions, path and title must have the same length but they are ${position.size} ${path.size} and ${title.size}" })
        val length = position.size
        val bookmarks = ArrayList<Chapter>(length)
        for (i in 0..length - 1) {
            bookmarks.add(Chapter(File(path[i]), title[i], position[i]))
        }
        return bookmarks
    }

    private fun convertToStringArray(from: Array<String>): IntArray {
        val out = IntArray(from.size)
        for (i in out.indices) {
            out[i] = Integer.valueOf(from[i])!!
        }
        return out
    }


    private fun byProjection(cursor: Cursor): Book {
        val rawDurations = cursor.string(KEY_CHAPTER_DURATIONS)
        val rawChapterNames = cursor.string(KEY_CHAPTER_NAMES)
        val rawChapterPaths = cursor.string(KEY_CHAPTER_PATHS)

        val chapterDurations = convertToStringArray(rawDurations.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        val chapterNames = rawChapterNames.split(stringSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val chapterPaths = rawChapterPaths.split(stringSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val chapters = generateChapters(chapterDurations, chapterPaths, chapterNames)
                .sorted()

        val bookId = cursor.long(BookTable.ID)
        val bookName = cursor.string(BookTable.NAME)
        val bookAuthor = cursor.stringNullable(BookTable.AUTHOR)
        val currentPath = File(cursor.string(BookTable.CURRENT_MEDIA_PATH))
        val bookSpeed = cursor.float(BookTable.PLAYBACK_SPEED)
        val bookRoot = cursor.string(BookTable.ROOT)
        val bookTime = cursor.int(BookTable.TIME)
        val bookType = Book.Type.valueOf(cursor.string(BookTable.TYPE))
        val bookUseCoverReplacement = cursor.int(BookTable.USE_COVER_REPLACEMENT) == BOOLEAN_TRUE

        return Book(bookId,
                bookType,
                bookUseCoverReplacement,
                bookAuthor,
                currentPath,
                bookTime,
                bookName,
                chapters,
                bookSpeed,
                bookRoot)
    }
}
