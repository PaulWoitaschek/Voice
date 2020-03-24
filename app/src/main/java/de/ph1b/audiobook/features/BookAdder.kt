package de.ph1b.audiobook.features

import android.Manifest
import android.content.Context
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.misc.FileRecognition
import de.ph1b.audiobook.misc.MediaAnalyzer
import de.ph1b.audiobook.misc.hasPermission
import de.ph1b.audiobook.misc.listFilesSafely
import de.ph1b.audiobook.misc.storageMounted
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class BookAdder
@Inject constructor(
  private val context: Context,
  private val repo: BookRepository,
  private val coverCollector: CoverFromDiscCollector,
  private val mediaAnalyzer: MediaAnalyzer,
  @Named(PrefKeys.SINGLE_BOOK_FOLDERS)
  private val singleBookFolderPref: Pref<Set<String>>,
  @Named(PrefKeys.COLLECTION_BOOK_FOLDERS)
  private val collectionBookFolderPref: Pref<Set<String>>
) {

  private val _scannerActive = ConflatedBroadcastChannel(false)
  val scannerActive: Flow<Boolean> = _scannerActive.asFlow()

  private val scanningDispatcher = newSingleThreadContext("bookAdder")

  init {
    GlobalScope.launch {
      combine(collectionBookFolderPref.flow, singleBookFolderPref.flow) { _, _ -> Unit }
        .collect {
          scanForFiles(restartIfScanning = true)
        }
    }
  }

  private var scanningJob: Job? = null

  fun scanForFiles(restartIfScanning: Boolean = false) {
    Timber.i("scanForFiles with restartIfScanning=$restartIfScanning")
    if (scanningJob?.isActive == true && !restartIfScanning) {
      return
    }
    GlobalScope.launch {
      scanningJob?.cancelAndJoin()
      scanningJob = launch(scanningDispatcher) {
        if (!context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          throw CancellationException("Does not have external storage permission")
        }
        _scannerActive.send(true)
        deleteOldBooks()
        measureTimeMillis {
          checkForBooks()
        }.also {
          Timber.i("checkForBooks took $it ms")
        }
        coverCollector.findCovers(repo.activeBooks())
      }.also {
        it.invokeOnCompletion {
          _scannerActive.offer(false)
        }
      }
    }
  }

  // check for new books
  private suspend fun checkForBooks() {
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

  private val singleBookFiles: List<File>
    get() = singleBookFolderPref.value
      .map(::File)
      .sortedWith(NaturalOrderComparator.fileComparator)

  private val collectionBookFiles: List<File>
    get() = collectionBookFolderPref.value
      .map(::File)
      .flatMap { it.listFilesSafely(FileRecognition.folderAndMusicFilter) }
      .sortedWith(NaturalOrderComparator.fileComparator)

  private suspend fun deleteOldBooks() {
    val singleBookFiles = singleBookFiles
    val collectionBookFolders = collectionBookFiles

    // getting books to remove
    val booksToRemove = ArrayList<Book>(20)
    for (book in repo.activeBooks()) {
      var bookExists = false
      when (book.type) {
        Book.Type.COLLECTION_FILE -> collectionBookFolders.forEach {
          if (it.isFile) {
            val chapters = book.content.chapters
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
            val chapters = book.content.chapters
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
      }

      if (!bookExists) {
        booksToRemove.add(book)
      }
    }

    if (!storageMounted()) {
      throw CancellationException("Storage is not mounted")
    }
    booksToRemove.forEach {
      repo.setBookActive(it.id, false)
    }
  }

  // adds a new book
  private suspend fun addNewBook(
    rootFile: File,
    bookId: UUID,
    newChapters: List<Chapter>,
    type: Book.Type
  ) {
    val bookRoot = if (rootFile.isDirectory) rootFile.absolutePath else rootFile.parent!!

    val firstChapterFile = newChapters.first().file
    val result =
      mediaAnalyzer.analyze(firstChapterFile) as? MediaAnalyzer.Result.Success ?: return

    var bookName = result.bookName
    if (bookName.isNullOrEmpty()) {
      bookName = result.chapterName
      if (bookName.isNullOrEmpty()) {
        val withoutExtension = rootFile.nameWithoutExtension
        bookName = if (withoutExtension.isEmpty()) {
          @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
          rootFile.name!!
        } else {
          withoutExtension
        }
      }
    }

    val existingBook = getBookFromDb(rootFile, type)
    if (existingBook == null) {
      val newBook = Book(
        id = bookId,
        metaData = BookMetaData(
          type = type,
          author = result.author,
          name = bookName,
          root = bookRoot,
          id = bookId,
          addedAtMillis = System.currentTimeMillis()
        ),
        content = BookContent(
          settings = BookSettings(
            currentFile = firstChapterFile,
            positionInChapter = 0,
            id = bookId,
            active = true,
            lastPlayedAtMillis = 0
          ),
          chapters = newChapters.withBookId(bookId),
          id = bookId
        )
      )
      repo.addBook(newBook)
    } else {
      // checks if current path is still valid.
      val oldCurrentFile = existingBook.content.currentFile
      val oldCurrentFileValid = newChapters.any { it.file == oldCurrentFile }

      // if the file is not valid, update time and position
      val time = if (oldCurrentFileValid) existingBook.content.positionInChapter else 0
      val currentFile = if (oldCurrentFileValid) {
        existingBook.content.currentFile
      } else {
        newChapters.first().file
      }

      val withUpdatedContent = existingBook.updateContent {
        copy(
          settings = settings.copy(
            positionInChapter = time,
            currentFile = currentFile
          ),
          chapters = newChapters.withBookId(existingBook.id)
        )
      }
      repo.addBook(withUpdatedContent)
    }
  }

  /** Updates a book. Adds the new chapters to the book and corrects the
   * [BookContent.currentFile] and [BookContent.positionInChapter]. **/
  private suspend fun updateBook(bookExisting: Book, newChapters: List<Chapter>) {
    var bookToUpdate = bookExisting.update(updateSettings = { copy(active = true) })
    val bookHasChanged = (bookToUpdate.content.chapters != newChapters) || !bookExisting.content.settings.active
    // sort chapters
    if (bookHasChanged) {
      // check if the chapter set as the current still exists
      var currentPathIsGone = true
      val currentFile = bookToUpdate.content.currentFile
      val currentTime = bookToUpdate.content.positionInChapter
      newChapters.forEach {
        if (it.file == currentFile) {
          if (it.duration < currentTime) {
            bookToUpdate = bookToUpdate.updateContent {
              copy(
                settings = settings.copy(positionInChapter = 0)
              )
            }
          }
          currentPathIsGone = false
        }
      }

      // set new bookmarks and chapters.
      // if the current path is gone, reset it correctly.
      bookToUpdate = bookToUpdate.updateContent {
        copy(
          chapters = newChapters.withBookId(bookToUpdate.id),
          settings = settings.copy(
            currentFile = if (currentPathIsGone) newChapters.first().file else currentFile,
            positionInChapter = if (currentPathIsGone) 0 else positionInChapter
          )
        )
      }
      repo.addBook(bookToUpdate)
    }
  }

  /** Adds a book if not there yet, updates it if there are changes or hides it if it does not
   * exist any longer **/
  private suspend fun checkBook(rootFile: File, type: Book.Type) {
    val bookExisting = getBookFromDb(rootFile, type)
    val bookId = bookExisting?.id ?: UUID.randomUUID()
    val newChapters = getChaptersByRootFile(bookId, rootFile)

    if (!storageMounted()) {
      throw CancellationException("Storage not mounted")
    }

    if (newChapters.isEmpty()) {
      // there are no chapters
      if (bookExisting != null) {
        // so delete book if available
        repo.setBookActive(bookExisting.id, false)
      }
    } else {
      // there are chapters
      if (bookExisting == null) {
        // there is no active book.
        addNewBook(rootFile, bookId, newChapters, type)
      } else {
        // there is a book, so update it if necessary
        updateBook(bookExisting, newChapters)
      }
    }
  }

  // Returns all the chapters matching to a Book root
  private suspend fun getChaptersByRootFile(bookId: UUID, rootFile: File): List<Chapter> {
    val containingFiles = rootFile.walk()
      .filter { FileRecognition.musicFilter.accept(it) }
      .sortedWith(NaturalOrderComparator.fileComparator)
      .toList()

    val containingMedia = ArrayList<Chapter>(containingFiles.size)
    for (f in containingFiles) {
      // check for existing chapter first so we can skip parsing
      val existingChapter = repo.chapterByFile(f)
      val lastModified = f.lastModified()
      if (existingChapter?.fileLastModified == lastModified) {
        containingMedia.add(existingChapter)
        continue
      }

      // else parse and add
      Timber.i("analyze $f")
      val result = mediaAnalyzer.analyze(f)
      Timber.i("analyzed $f.")
      if (result is MediaAnalyzer.Result.Success) {
        containingMedia.add(
          Chapter(
            file = f,
            name = result.chapterName,
            duration = result.duration,
            fileLastModified = lastModified,
            markData = result.chapters,
            bookId = bookId
          )
        )
      }
    }
    return containingMedia
  }

  private fun getBookFromDb(rootFile: File, type: Book.Type): Book? {
    val books = repo.allBooks()
    if (rootFile.isDirectory) {
      return books.firstOrNull {
        rootFile.absolutePath == it.root && type === it.type
      }
    } else if (rootFile.isFile) {
      for (b in books) {
        if (rootFile.parentFile?.absolutePath == b.root && type === b.type) {
          val singleChapter = b.content.chapters.first()
          if (singleChapter.file == rootFile) {
            return b
          }
        }
      }
    }
    return null
  }
}

private fun Iterable<Chapter>.withBookId(bookId: UUID): List<Chapter> {
  return map { chapter ->
    if (chapter.bookId == bookId) {
      chapter
    } else {
      chapter.copy(bookId = bookId)
    }
  }
}
