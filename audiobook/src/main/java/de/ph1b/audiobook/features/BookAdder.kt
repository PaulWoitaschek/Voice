package de.ph1b.audiobook.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.content.ContextCompat
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Base class for adding new books.
 *
 * @author Paul Woitaschek
 */
@Singleton class BookAdder
@Inject constructor(private val context: Context, private val prefs: PrefsManager, private val repo: BookRepository, private val coverCollector: CoverFromDiscCollector) {

  private val executor = Executors.newSingleThreadExecutor()
  private val scannerActiveSubject = BehaviorSubject.createDefault(false)
  val scannerActive: Observable<Boolean> = scannerActiveSubject
  private val handler = Handler(context.mainLooper)
  @Volatile private var stopScanner = false

  // check for new books
  @Throws(InterruptedException::class)
  private fun checkForBooks() {
    val singleBooks = singleBookFiles
    for (f in singleBooks) {
      if (f.isFile && f.canRead()) {
        checkBook(f, Book.Type.SINGLE_FILE)
      } else if (f.isDirectory && f.canRead()) {
        checkBook(f, Book.Type.SINGLE_FOLDER)
      }
    }

    val collectionBooks = collectionBookFiles
    for (f in collectionBooks) {
      if (f.isFile && f.canRead()) {
        checkBook(f, Book.Type.COLLECTION_FILE)
      } else if (f.isDirectory && f.canRead()) {
        checkBook(f, Book.Type.COLLECTION_FOLDER)
      }
    }
  }

  /**
   * Starts scanning for new [Book] or changes within.
   *
   * @param interrupting true if a eventually running scanner should be interrupted.
   */
  fun scanForFiles(interrupting: Boolean) {
    if (!scannerActiveSubject.value || interrupting) {
      stopScanner = true
      executor.execute {
        handler.postBlocking { scannerActiveSubject.onNext(true) }
        stopScanner = false

        try {
          deleteOldBooks()
          checkForBooks()
          coverCollector.findCovers(repo.activeBooks)
        } catch (ignored: InterruptedException) {
        }

        stopScanner = false
        handler.postBlocking { scannerActiveSubject.onNext(false) }
      }
    }
  }

  /** the saved single book files the User chose in [de.ph1b.audiobook.features.folderChooser.FolderChooserView] */
  private val singleBookFiles: List<File>
    get() = prefs.singleBookFolders.value()
      .map(::File)
      .sortedWith(NaturalOrderComparator.fileComparator)

  // Gets the saved collection book files the User chose in [FolderChooserView]
  private val collectionBookFiles: List<File>
    get() = prefs.collectionFolders.value()
      .map(::File)
      .flatMap { it.listFilesSafely(FileRecognition.folderAndMusicFilter) }
      .sortedWith(NaturalOrderComparator.fileComparator)

  /** Deletes all the books that exist on the database but not on the hard drive or on the saved
   * audio book paths. **/
  @Throws(InterruptedException::class)
  private fun deleteOldBooks() {
    val singleBookFiles = singleBookFiles
    val collectionBookFolders = collectionBookFiles

    //getting books to remove
    val booksToRemove = ArrayList<Book>(20)
    for (book in repo.activeBooks) {
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
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      throw InterruptedException("Does not have external storage permission")
    }

    handler.postBlocking { repo.hideBook(booksToRemove) }
  }

  // adds a new book
  private fun addNewBook(rootFile: File, newChapters: List<Chapter>, type: Book.Type) {
    val bookRoot = if (rootFile.isDirectory)
      rootFile.absolutePath
    else
      rootFile.parent

    val firstChapterFile = newChapters.first().file
    val result = MediaAnalyzer.compute(firstChapterFile)
    var bookName = result.bookName
    if (bookName.isNullOrEmpty()) {
      val withoutExtension = rootFile.nameWithoutExtension
      bookName = if (withoutExtension.isEmpty()) rootFile.name else withoutExtension
    }
    bookName!!

    var orphanedBook = getBookFromDb(rootFile, type, true)
    if (orphanedBook == null) {
      val newBook = Book(
        Book.ID_UNKNOWN,
        type,
        result.author,
        firstChapterFile,
        0,
        bookName,
        newChapters,
        1.0f,
        bookRoot)
      handler.postBlocking { repo.addBook(newBook) }
    } else {
      // checks if current path is still valid.
      val oldCurrentFile = orphanedBook.currentFile
      val oldCurrentFileValid = newChapters.any { it.file == oldCurrentFile }

      // if the file is not valid, update time and position
      val time = if (oldCurrentFileValid) orphanedBook.time else 0
      val currentFile = if (oldCurrentFileValid) orphanedBook.currentFile else newChapters.first().file

      orphanedBook = orphanedBook.copy(time = time, currentFile = currentFile, chapters = newChapters)

      // now finally un-hide this book
      handler.postBlocking { repo.revealBook(orphanedBook as Book) }
    }
  }

  /** Updates a book. Adds the new chapters to the book and corrects the
   * [Book.currentFile] and [Book.time]. **/
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

      handler.postBlocking { repo.updateBook(bookToUpdate) }
    }
  }

  /** Adds a book if not there yet, updates it if there are changes or hides it if it does not
   * exist any longer **/
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
        handler.postBlocking { repo.hideBook(listOf(bookExisting)) }
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

  // Returns all the chapters matching to a Book root
  @Throws(InterruptedException::class)
  private fun getChaptersByRootFile(rootFile: File): List<Chapter> {
    val containingFiles = rootFile.walk()
      .filter { FileRecognition.musicFilter.accept(it) }
      .toMutableList()
      .sortedWith(NaturalOrderComparator.fileComparator)

    val containingMedia = ArrayList<Chapter>(containingFiles.size)
    for (f in containingFiles) {
      val result = MediaAnalyzer.compute(f)
      if (result.duration > 0) {
        containingMedia.add(Chapter(f, result.chapterName, result.duration))
      }
      throwIfStopRequested()
    }
    return containingMedia
  }

  // Throws an interruption if [.stopScanner] is true.
  @Throws(InterruptedException::class)
  private fun throwIfStopRequested() {
    if (stopScanner) {
      throw InterruptedException("Interruption requested")
    }
  }


  /**
   * Gets a book from the database matching to a defines mask.
   *
   * @param orphaned If we should return a book that is orphaned, or a book that is currently
   */
  private fun getBookFromDb(rootFile: File, type: Book.Type, orphaned: Boolean): Book? {
    val books: List<Book> =
      if (orphaned) {
        repo.getOrphanedBooks()
      } else {
        repo.activeBooks
      }
    if (rootFile.isDirectory) {
      return books.firstOrNull {
        rootFile.absolutePath == it.root && type === it.type
      }
    } else if (rootFile.isFile) {
      for (b in books) {
        if (rootFile.parentFile.absolutePath == b.root && type === b.type) {
          val singleChapter = b.chapters.first()
          if (singleChapter.file == rootFile) {
            return b
          }
        }
      }
    }
    return null
  }

  private inline fun Handler.postBlocking(crossinline func: () -> Any) {
    val cdl = CountDownLatch(1)
    post {
      func()
      cdl.countDown()
    }
    cdl.await()
  }
}
