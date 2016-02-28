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

package de.ph1b.audiobook.model

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.support.v4.content.ContextCompat
import d
import de.ph1b.audiobook.activity.BaseActivity
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import de.ph1b.audiobook.utils.BookVendor
import de.ph1b.audiobook.utils.FileRecognition
import de.ph1b.audiobook.utils.MediaAnalyzer
import rx.subjects.BehaviorSubject
import v
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Base class for adding new books.

 * @author Paul Woitaschek
 */
@Singleton
class BookAdder
@Inject
constructor(private val context: Context, private val prefs: PrefsManager, private val db: BookChest, private val bookVendor: BookVendor, private val mediaAnalyzer: MediaAnalyzer, private val coverCollector: CoverFromDiscCollector) {

    private val executor = Executors.newSingleThreadExecutor()
    private val scannerActive = BehaviorSubject.create(false)
    private val handler = Handler(context.mainLooper)
    @Volatile private var stopScanner = false

    fun scannerActive(): BehaviorSubject<Boolean> {
        return scannerActive
    }

    /**
     * Checks for new books

     * @throws InterruptedException if a reset on the scanner has been requested
     */
    @Throws(InterruptedException::class)
    private fun checkForBooks() {
        val singleBooks = singleBookFiles
        for (f in singleBooks) {
            d { "checkForBooks with singleBookFile=$f" }
            if (f.isFile && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FILE)
            } else if (f.isDirectory && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FOLDER)
            }
        }

        val collectionBooks = collectionBookFiles
        for (f in collectionBooks) {
            d { "checking collectionBook=$f" }
            if (f.isFile && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FILE)
            } else if (f.isDirectory && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FOLDER)
            }
        }
    }


    /**
     * Starts scanning for new [Book] or changes within.

     * @param interrupting true if a eventually running scanner should be interrupted.
     */
    fun scanForFiles(interrupting: Boolean) {
        d { "scanForFiles called with scannerActive=${scannerActive.value} and interrupting=$interrupting" }
        if (!scannerActive.value || interrupting) {
            stopScanner = true
            executor.execute {
                v { "started" }
                handler.post { scannerActive.onNext(true) }
                stopScanner = false

                try {
                    deleteOldBooks()
                    checkForBooks()
                    coverCollector.findCovers(bookVendor.all())
                } catch (ex: InterruptedException) {
                    d(ex) { "We were interrupted at adding a book" }
                }

                stopScanner = false
                handler.post { scannerActive.onNext(false) }
                v { "stopped" }
            }
        }
        v { "scanForFiles method done (executor should be called" }
    }

    /**
     * Gets the saved single book files the User chose in [FolderChooserView]

     * @return An array of chosen single book folders.
     * *
     * @see Book.Type.SINGLE_FILE

     * @see Book.Type.SINGLE_FOLDER
     */
    private val singleBookFiles: List<File>
        get() {
            val singleBooksAsStrings = prefs.singleBookFolders
            val singleBooks = ArrayList<File>(singleBooksAsStrings.size)
            for (s in singleBooksAsStrings) {
                singleBooks.add(File(s))
            }
            return singleBooks.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
        }

    /**
     * Gets the saved collection book files the User chose in [FolderChooserView]

     * @return An array of chosen collection book folders.
     * *
     * @see Book.Type.COLLECTION_FILE

     * @see Book.Type.COLLECTION_FOLDER
     */
    private val collectionBookFiles: List<File>
        get() {
            val collectionFoldersStringList = prefs.collectionFolders
            val containingFiles = ArrayList<File>(collectionFoldersStringList.size)
            for (s in collectionFoldersStringList) {
                val f = File(s)
                if (f.exists() && f.isDirectory) {
                    val containing = f.listFiles(FileRecognition.folderAndMusicFilter)
                    if (containing != null) {
                        containingFiles.addAll(Arrays.asList(*containing))
                    }
                }
            }
            return containingFiles.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
        }

    /**
     * Deletes all the books that exist on the database but not on the hard drive or on the saved
     * audio book paths.
     */
    @Throws(InterruptedException::class)
    private fun deleteOldBooks() {
        d { "deleteOldBooks started" }
        val singleBookFiles = singleBookFiles
        val collectionBookFolders = collectionBookFiles

        //getting books to remove
        val booksToRemove = ArrayList<Book>(20)
        for (book in bookVendor.all()) {
            var bookExists = false
            when (book.type) {
                Book.Type.COLLECTION_FILE -> collectionBookFolders.forEach {
                    if (it.isFile) {
                        val chapters = book.chapters
                        val singleBookChapterFile = chapters.first().file
                        if (singleBookChapterFile == it) {
                            bookExists = true
                        }
                    }
                }
                Book.Type.COLLECTION_FOLDER -> collectionBookFolders.forEach {
                    if (it.isDirectory) {
                        // multi file book
                        if (book.root == it.absolutePath) {
                            bookExists = true
                        }
                    }
                }
                Book.Type.SINGLE_FILE -> singleBookFiles.forEach {
                    if (it.isFile) {
                        val chapters = book.chapters
                        val singleBookChapterFile = chapters.first().file
                        if (singleBookChapterFile == it) {
                            bookExists = true
                        }
                    }
                }
                Book.Type.SINGLE_FOLDER -> singleBookFiles.forEach {
                    if (it.isDirectory) {
                        // multi file book
                        if (book.root == it.absolutePath) {
                            bookExists = true
                        }
                    }
                }
                else -> throw AssertionError("We added somewhere a non valid type=" + book.type)
            }

            if (!bookExists) {
                booksToRemove.add(book)
            }
        }

        if (!BaseActivity.storageMounted()) {
            throw InterruptedException("Storage is not mounted")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                throw InterruptedException("Does not have external storage permission")
            }
        }

        for (b in booksToRemove) {
            d { "deleting book=${b.name}" };
            handler.post { db.hideBook(b) }
        }
    }

    /**
     * Adds a new book

     * @param rootFile    The root of the book
     * *
     * @param newChapters The new chapters that have been found matching to the location of the book
     * *
     * @param type        The type of the book
     */
    private fun addNewBook(rootFile: File, newChapters: List<Chapter>, type: Book.Type) {
        val bookRoot = if (rootFile.isDirectory)
            rootFile.absolutePath
        else
            rootFile.parent

        val firstChapterFile = newChapters.first().file
        val result = mediaAnalyzer.compute(firstChapterFile)
        var bookName = result.bookName
        if (bookName.isNullOrEmpty()) {
            val withoutExtension = rootFile.nameWithoutExtension
            bookName = if (withoutExtension.isEmpty()) rootFile.name else withoutExtension
        }
        bookName!!

        var orphanedBook = getBookFromDb(rootFile, type, true)
        if (orphanedBook == null) {
            val newBook = Book(
                    Book.ID_UNKNOWN.toLong(),
                    type,
                    false,
                    result.author,
                    firstChapterFile,
                    0,
                    bookName,
                    newChapters,
                    1.0f,
                    bookRoot)
            d { "adding newBook=${newBook.name}" }
            handler.post { db.addBook(newBook) }
        } else {
            orphanedBook = orphanedBook.copy(chapters = newChapters)

            // checks if current path is still valid. if not, reset position.
            val currentFile = orphanedBook.currentFile
            val pathValid = orphanedBook.chapters.any { it.file == currentFile }
            if (!pathValid) {
                orphanedBook = orphanedBook.copy(currentFile = orphanedBook.chapters.first().file,
                        time = 0)
            }

            // now finally un-hide this book
            handler.post { db.revealBook(orphanedBook as Book) }
        }
    }

    /**
     * Updates a book. Adds the new chapters to the book and corrects the
     * [Book.currentFile] and [Book.time].

     * @param bookExisting The existing book
     * *
     * @param newChapters  The new chapters matching to the book
     */
    private fun updateBook(bookExisting: Book, newChapters: List<Chapter>) {
        var bookToUpdate = bookExisting
        val bookHasChanged = bookToUpdate.chapters != newChapters
        // sort chapters
        if (bookHasChanged) {
            // check if the chapter set as the current still exists
            var currentPathIsGone = true
            val currentFile = bookToUpdate.currentFile
            val currentTime = bookToUpdate.time
            newChapters.forEach {
                if (it.file == currentFile) {
                    if (it.duration < currentTime) {
                        bookToUpdate = bookToUpdate.copy(time = 0)
                    }
                    currentPathIsGone = false
                }
            }

            //set new bookmarks and chapters.
            // if the current path is gone, reset it correctly.
            bookToUpdate = bookToUpdate.copy(
                    chapters = newChapters,
                    currentFile = if (currentPathIsGone) newChapters.first().file else bookToUpdate.currentFile,
                    time = if (currentPathIsGone) 0 else bookToUpdate.time)

            handler.post { db.updateBook(bookToUpdate) }
        }
    }

    /**
     * Adds a book if not there yet, updates it if there are changes or hides it if it does not
     * exist any longer

     * @param rootFile The Book root
     * *
     * @param type     The type of the book
     * *
     * @throws InterruptedException If the scanner has been requested to reset
     */
    @Throws(InterruptedException::class)
    private fun checkBook(rootFile: File, type: Book.Type) {
        val newChapters = getChaptersByRootFile(rootFile)
        val bookExisting = getBookFromDb(rootFile, type, false)

        if (!BaseActivity.storageMounted()) {
            throw InterruptedException("Storage not mounted")
        }

        if (newChapters.isEmpty()) {
            // there are no chapters
            if (bookExisting != null) {
                //so delete book if available
                handler.post { db.hideBook(bookExisting) }
            }
        } else {
            // there are chapters
            if (bookExisting == null) {
                //there is no active book.
                addNewBook(rootFile, newChapters, type)
            } else {
                //there is a book, so update it if necessary
                updateBook(bookExisting, newChapters)
            }
        }
    }

    /**
     * Returns all the chapters matching to a Book root

     * @param rootFile The root of the book
     * *
     * @return The chapters
     * *
     * @throws InterruptedException If the scanner has been requested to terminate
     */
    @Throws(InterruptedException::class)
    private fun getChaptersByRootFile(rootFile: File): List<Chapter> {
        val containingFiles = rootFile.walk()
                .filter { FileRecognition.musicFilter.accept(it) }
                .toMutableList()
                .sortedWith(NaturalOrderComparator.FILE_COMPARATOR)

        val containingMedia = ArrayList<Chapter>(containingFiles.size)
        for (f in containingFiles) {
            val result = mediaAnalyzer.compute(f)
            if (result.duration > 0) {
                containingMedia.add(Chapter(f, result.chapterName, result.duration))
            }
            throwIfStopRequested()
        }
        return containingMedia
    }

    /**
     * Throws an interruption if [.stopScanner] is true.

     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    private fun throwIfStopRequested() {
        if (stopScanner) {
            throw InterruptedException("Interruption requested")
        }
    }


    /**
     * Gets a book from the database matching to a defines mask.

     * @param rootFile The root of the book
     * *
     * @param type     The type of the book
     * *
     * @param orphaned If we sould return a book that is orphaned, or a book that is currently
     * *                 active
     * *
     * @return The Book if available, or `null`
     */
    private fun getBookFromDb(rootFile: File, type: Book.Type, orphaned: Boolean): Book? {
        d { "getBookFromDb, rootFile=$rootFile, type=$type, orphaned=$orphaned" }
        val books: List<Book> =
                if (orphaned) {
                    db.getOrphanedBooks()
                } else {
                    bookVendor.all()
                }
        if (rootFile.isDirectory) {
            for (b in books) {
                if (rootFile.absolutePath == b.root && type === b.type) {
                    return b
                }
            }
        } else if (rootFile.isFile) {
            d { "getBookFromDb, its a file" }
            for (b in books) {
                v { "Comparing bookRoot=${b.root} with ${rootFile.parentFile.absoluteFile}" }
                if (rootFile.parentFile.absolutePath == b.root && type === b.type) {
                    val singleChapter = b.chapters.first()
                    d { "getBookFromDb, singleChapterPath=${singleChapter.file} compared with=${rootFile.absoluteFile}" }
                    if (singleChapter.file == rootFile) {
                        return b
                    }
                }
            }
        }
        return null
    }
}
