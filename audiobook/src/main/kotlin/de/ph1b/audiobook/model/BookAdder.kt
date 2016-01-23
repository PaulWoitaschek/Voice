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
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.content.ContextCompat
import com.google.common.collect.Collections2
import com.google.common.io.Files
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.activity.BaseActivity
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.utils.BookVendor
import de.ph1b.audiobook.utils.FileRecognition
import de.ph1b.audiobook.utils.MediaAnalyzer
import rx.subjects.BehaviorSubject
import timber.log.Timber
import java.io.File
import java.io.IOException
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
constructor(private val c: Context, private val prefs: PrefsManager, private val db: BookChest, private val bookVendor: BookVendor, private val activityManager: ActivityManager, private val imageHelper: ImageHelper, private val mediaAnalyzer: MediaAnalyzer) {

    private val executor = Executors.newSingleThreadExecutor()
    private val scannerActive = BehaviorSubject.create(false)
    @Volatile private var stopScanner = false

    /**
     * Adds files recursively. First takes all files and adds them sorted to the return list. Then
     * sorts the folders, and then adds their content sorted to the return list.

     * @param source The dirs and files to be added
     * *
     * @param audio  True if audio should be filtered. Else images will be filtered
     * *
     * @return All the files containing in a natural sorted order.
     */
    private fun getAllContainingFiles(source: List<File>, audio: Boolean): List<File> {
        // split the files in dirs and files
        val fileList = ArrayList<File>(source.size)
        for (f in source) {
            if (f.isFile) {
                fileList.add(f)
            } else if (f.isDirectory) {
                // recursively add the content of the directory
                val containing = f.listFiles(if (audio) FileRecognition.folderAndMusicFilter else FileRecognition.folderAndImagesFilter)
                if (containing != null) {
                    val content = ArrayList(Arrays.asList(*containing))
                    fileList.addAll(getAllContainingFiles(content, audio))
                }
            }
        }

        // return all the files only^
        return fileList
    }

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
            Timber.d("checkForBooks with singleBookFile=%s", f)
            if (f.isFile && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FILE)
            } else if (f.isDirectory && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FOLDER)
            }
        }

        val collectionBooks = collectionBookFiles
        for (f in collectionBooks) {
            Timber.d("checking collectionBook=%s", f)
            if (f.isFile && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FILE)
            } else if (f.isDirectory && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FOLDER)
            }
        }
    }

    /**
     * Returns a Bitmap from an array of [File] that should be images

     * @param coverFiles The image files to check
     * *
     * @return A bitmap or `null` if there is none.
     * *
     * @throws InterruptedException If the scanner has been requested to reset.
     */
    @Throws(InterruptedException::class)
    private fun getCoverFromDisk(coverFiles: List<File>): Bitmap? {
        // if there are images, get the first one.
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        val dimen = imageHelper.smallerScreenSize
        for (f in coverFiles) {
            throwIfStopRequested()
            // only read cover if its size is less than a third of the available memory
            if (f.length() < (mi.availMem / 3L)) {
                try {
                    return Picasso.with(c).load(f).resize(dimen, dimen).get()
                } catch (e: IOException) {
                    Timber.e(e, "Error when saving cover %s", f)
                }
            }
        }
        return null
    }

    /**
     * Finds an embedded cover within a [Chapter]

     * @param chapters The chapters to search trough
     * *
     * @return An embedded cover if there is one. Else return `null`
     * *
     * @throws InterruptedException If the scanner has been requested to reset.
     */
    @Throws(InterruptedException::class)
    private fun getEmbeddedCover(chapters: List<Chapter>): Bitmap? {
        var tries = 0
        val maxTries = 5
        for (c in chapters) {
            if (++tries < maxTries) {
                throwIfStopRequested()
                val cover = imageHelper.getEmbeddedCover(c.file)
                if (cover != null) {
                    return cover
                }
            } else {
                return null
            }
        }
        return null
    }

    /**
     * Trys to find covers and saves them to storage if found.

     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    private fun findCovers() {
        for (b in bookVendor.all()) {
            throwIfStopRequested()
            val coverFile = b.coverFile()
            if (!coverFile.exists()) {
                if (b.type === Book.Type.COLLECTION_FOLDER || b.type === Book.Type.SINGLE_FOLDER) {
                    val root = File(b.root)
                    if (root.exists()) {
                        val images = getAllContainingFiles(listOf(root), false)
                        val cover = getCoverFromDisk(images)
                        if (cover != null) {
                            imageHelper.saveCover(cover, coverFile)
                            Picasso.with(c).invalidate(coverFile)
                            db.updateBook(b)
                            continue
                        }
                    }
                }
                val cover = getEmbeddedCover(b.chapters)
                if (cover != null) {
                    imageHelper.saveCover(cover, coverFile)
                    Picasso.with(c).invalidate(coverFile)
                    db.updateBook(b)
                }
            }
        }
    }

    /**
     * Starts scanning for new [Book] or changes within.

     * @param interrupting true if a eventually running scanner should be interrupted.
     */
    fun scanForFiles(interrupting: Boolean) {
        Timber.d("scanForFiles called with scannerActive=${scannerActive.value} and interrupting=$interrupting")
        if (!scannerActive.value || interrupting) {
            stopScanner = true
            executor.execute {
                Timber.v("started")
                scannerActive.onNext(true)
                stopScanner = false

                try {
                    deleteOldBooks()
                    checkForBooks()
                    findCovers()
                } catch (e: InterruptedException) {
                    Timber.d(e, "We were interrupted at adding a book")
                }

                stopScanner = false
                scannerActive.onNext(false)
                Timber.v("stopped")
            }
        }
        Timber.v("scanForFiles method done (executor should be called")
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
        Timber.d("deleteOldBooks started")
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
            if (ContextCompat.checkSelfPermission(c, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                throw InterruptedException("Does not have external storage permission")
            }
        }

        for (b in booksToRemove) {
            Timber.d("deleting book=${b.name}");
            db.hideBook(b)
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
            val withoutExtension = Files.getNameWithoutExtension(rootFile.absolutePath)
            bookName = if (withoutExtension.isEmpty()) rootFile.name else withoutExtension
        }
        bookName!!

        var orphanedBook = getBookFromDb(rootFile, type, true)
        if (orphanedBook == null) {
            val newBook = Book(
                    Book.ID_UNKNOWN.toLong(),
                    emptyList(),
                    type,
                    false,
                    result.author,
                    firstChapterFile,
                    0,
                    bookName,
                    newChapters,
                    1.0f,
                    bookRoot)
            Timber.d("adding newBook=${newBook.name}")
            db.addBook(newBook)
        } else {
            // restore old books
            // now removes invalid bookmarks
            val filteredBookmarks = ArrayList(orphanedBook.bookmarks.filter {
                for (c in newChapters) {
                    if (c.file == it.mediaFile) {
                        return@filter true
                    }
                }
                false
            })
            orphanedBook = orphanedBook.copy(bookmarks = filteredBookmarks,
                    chapters = newChapters)

            // checks if current path is still valid. if not, reset position.
            var pathValid = false
            for (c in orphanedBook.chapters) {
                if (c.file == orphanedBook.currentFile) {
                    pathValid = true
                }
            }
            if (!pathValid) {
                orphanedBook = orphanedBook.copy(currentFile = orphanedBook.chapters.first().file,
                        time = 0)
            }

            // now finally un-hide this book
            db.revealBook(orphanedBook)
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

            // removes the bookmarks that no longer represent an existing file
            val filteredBookmarks = ArrayList(Collections2.filter(bookToUpdate.bookmarks) {
                for (c in newChapters) {
                    if (c.file == it.mediaFile) {
                        return@filter true
                    }
                }
                false
            })

            //set new bookmarks and chapters.
            // if the current path is gone, reset it correctly.
            bookToUpdate = bookToUpdate.copy(
                    bookmarks = filteredBookmarks,
                    chapters = newChapters,
                    currentFile = if (currentPathIsGone) newChapters.first().file else bookToUpdate.currentFile,
                    time = if (currentPathIsGone) 0 else bookToUpdate.time)

            db.updateBook(bookToUpdate)
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
                db.hideBook(bookExisting)
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
        val containingFiles = getAllContainingFiles(listOf(rootFile), true)
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
        Timber.d("getBookFromDb, rootFile=$rootFile, type=$type, orphaned=$orphaned")
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
            Timber.d("getBookFromDb, its a file")
            for (b in books) {
                Timber.v("Comparing bookRoot=${b.root} with ${rootFile.parentFile.absoluteFile}")
                if (rootFile.parentFile.absolutePath == b.root && type === b.type) {
                    val singleChapter = b.chapters.first()
                    Timber.d("getBookFromDb, singleChapterPath=%s compared with=%s", singleChapter.file, rootFile.absoluteFile)
                    if (singleChapter.file == rootFile) {
                        return b
                    }
                }
            }
        }
        return null
    }
}
