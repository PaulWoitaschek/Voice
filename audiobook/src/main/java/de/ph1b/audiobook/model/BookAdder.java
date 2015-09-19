package de.ph1b.audiobook.model;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.FileRecognition;
import de.ph1b.audiobook.utils.L;


/**
 * Base class for adding new books.
 *
 * @author Paul Woitaschek
 */
public class BookAdder {

    private static final String TAG = BookAdder.class.getSimpleName();
    private static final Communication COMMUNICATION = Communication.getInstance();
    public static volatile boolean scannerActive = false;
    private static BookAdder instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context c;
    private final PrefsManager prefs;
    private final DataBaseHelper db;
    private volatile boolean stopScanner = false;

    private BookAdder(@NonNull Context c) {
        this.c = c;
        prefs = PrefsManager.getInstance(c);
        db = DataBaseHelper.getInstance(c);
    }

    public static synchronized BookAdder getInstance(Context c) {
        if (instance == null) {
            instance = new BookAdder(c.getApplicationContext());
        }
        return instance;
    }

    /**
     * Returns the name of the book we want to add. If there is a tag embedded, use that one. Else
     * derive the title from the filename.
     *
     * @param firstChapterFile A path to a file
     * @param rootFile         The root of the book to add
     * @return The name of the book we add
     */
    @NonNull
    private static String getBookName(@NonNull File firstChapterFile, @NonNull File rootFile, @NonNull MediaMetadataRetriever mmr) {
        String bookName = null;
        try {
            mmr.setDataSource(firstChapterFile.getAbsolutePath());
            bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        } catch (RuntimeException ignored) {
        }
        if (bookName == null || bookName.isEmpty()) {
            String withoutExtension = Files.getNameWithoutExtension(rootFile.getAbsolutePath());
            bookName = withoutExtension.isEmpty() ? rootFile.getName() : withoutExtension;
        }
        return bookName;
    }

    /**
     * Returns the author of the book we want to add. If there is a tag embedded, use that one. Else
     * return null
     *
     * @param firstChapterFile A path to a file
     * @return The name of the book we add
     */
    @Nullable
    private static String getAuthor(@NonNull File firstChapterFile, @NonNull MediaMetadataRetriever mmr) {
        try {
            mmr.setDataSource(firstChapterFile.getAbsolutePath());
            String bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            if (bookName == null || bookName.isEmpty()) {
                bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
            }
            if (bookName == null || bookName.isEmpty()) {
                bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            }
            return bookName;
        } catch (RuntimeException ignored) {
            return null;
        }
    }


    /**
     * Adds files recursively. First takes all files and adds them sorted to the return list. Then
     * sorts the folders, and then adds their content sorted to the return list.
     *
     * @param source The dirs and files to be added
     * @param audio  True if audio should be filtered. Else images will be filtered
     * @return All the files containing in a natural sorted order.
     */
    private static List<File> getAllContainingFiles(@NonNull List<File> source, boolean audio) {
        // split the files in dirs and files
        List<File> fileList = new ArrayList<>(source.size());
        for (File f : source) {
            if (f.isFile()) {
                fileList.add(f);
            } else if (f.isDirectory()) {
                // recursively add the content of the directory
                File[] containing = f.listFiles(audio ?
                        FileRecognition.FOLDER_AND_MUSIC_FILTER :
                        FileRecognition.FOLDER_AND_IMAGES_FILTER);
                if (containing != null) {
                    List<File> content = new ArrayList<>(Arrays.asList(containing));
                    fileList.addAll(getAllContainingFiles(content, audio));
                }
            }
        }

        // return all the files only^
        return fileList;
    }

    /**
     * Checks for new books
     *
     * @throws InterruptedException if a reset on the scanner has been requested
     */
    private void checkForBooks() throws InterruptedException {
        List<File> singleBooks = getSingleBookFiles();
        for (File f : singleBooks) {
            L.d(TAG, "checkForBooks with singleBookFile=" + f);
            if (f.isFile() && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FILE);
            } else if (f.isDirectory() && f.canRead()) {
                checkBook(f, Book.Type.SINGLE_FOLDER);
            }
        }

        List<File> collectionBooks = getCollectionBookFiles();
        for (File f : collectionBooks) {
            L.d(TAG, "checking collectionBook=" + f);
            if (f.isFile() && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FILE);
            } else if (f.isDirectory() && f.canRead()) {
                checkBook(f, Book.Type.COLLECTION_FOLDER);
            }
        }
    }

    /**
     * Returns a Bitmap from an array of {@link File} that should be images
     *
     * @param coverFiles The image files to check
     * @return A bitmap or {@code null} if there is none.
     * @throws InterruptedException If the scanner has been requested to reset.
     */
    @Nullable
    private Bitmap getCoverFromDisk(@NonNull List<File> coverFiles) throws InterruptedException {
        // if there are images, get the first one.
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        int dimen = ImageHelper.getSmallerScreenSize(c);
        for (File f : coverFiles) {
            throwIfStopRequested();
            // only read cover if its size is less than a third of the available memory
            if (f.length() < (mi.availMem / 3L)) {
                try {
                    return Picasso.with(c).load(f).resize(dimen, dimen).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Finds an embedded cover within a {@link Chapter}
     *
     * @param chapters The chapters to search trough
     * @return An embedded cover if there is one. Else return {@code null}
     * @throws InterruptedException If the scanner has been requested to reset.
     */
    @Nullable
    private Bitmap getEmbeddedCover(@NonNull List<Chapter> chapters) throws InterruptedException {
        int tries = 0;
        int maxTries = 5;
        for (Chapter c : chapters) {
            if (++tries < maxTries) {
                throwIfStopRequested();
                Bitmap cover = ImageHelper.getEmbeddedCover(c.getFile(), this.c);
                if (cover != null) {
                    return cover;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Trys to find covers and saves them to storage if found.
     *
     * @throws InterruptedException
     */
    private void findCovers() throws InterruptedException {
        for (Book b : db.getActiveBooks()) {
            throwIfStopRequested();
            File coverFile = b.getCoverFile();
            if (!coverFile.exists()) {
                if (b.getType() == Book.Type.COLLECTION_FOLDER || b.getType() == Book.Type.SINGLE_FOLDER) {
                    File root = new File(b.getRoot());
                    if (root.exists()) {
                        List<File> images = getAllContainingFiles(Collections.singletonList(root), false);
                        Bitmap cover = getCoverFromDisk(images);
                        if (cover != null) {
                            ImageHelper.saveCover(cover, c, coverFile);
                            Picasso.with(c).invalidate(coverFile);
                            COMMUNICATION.sendBookContentChanged(b);
                            continue;
                        }
                    }
                }
                Bitmap cover = getEmbeddedCover(b.getChapters());
                if (cover != null) {
                    ImageHelper.saveCover(cover, c, coverFile);
                    Picasso.with(c).invalidate(coverFile);
                    COMMUNICATION.sendBookContentChanged(b);
                }
            }
        }
    }

    /**
     * Starts scanning for new {@link Book} or changes within.
     *
     * @param interrupting true if a eventually running scanner should be interrupted.
     */
    public void scanForFiles(boolean interrupting) {
        L.d(TAG, "scanForFiles called. scannerActive=" + scannerActive + ", interrupting=" + interrupting);
        if (!scannerActive || interrupting) {
            stopScanner = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    L.v(TAG, "started");
                    scannerActive = true;
                    COMMUNICATION.sendScannerStateChanged();
                    stopScanner = false;

                    try {
                        deleteOldBooks();
                        checkForBooks();
                        findCovers();
                    } catch (InterruptedException e) {
                        L.d(TAG, "We were interrupted at adding a book", e);
                    }

                    stopScanner = false;
                    scannerActive = false;
                    COMMUNICATION.sendScannerStateChanged();
                    L.v(TAG, "stopped");
                }
            });
        }
        L.v(TAG, "scanforfiles method done (executor should be called");
    }

    /**
     * Gets the saved single book files the User chose in {@link de.ph1b.audiobook.activity.FolderChooserActivity}
     *
     * @return An array of chosen single book folders.
     * @see de.ph1b.audiobook.model.Book.Type#SINGLE_FILE
     * @see de.ph1b.audiobook.model.Book.Type#SINGLE_FOLDER
     */
    @NonNull
    private List<File> getSingleBookFiles() {
        List<String> singleBooksAsStrings = prefs.getSingleBookFolders();
        List<File> singleBooks = new ArrayList<>(singleBooksAsStrings.size());
        for (String s : singleBooksAsStrings) {
            singleBooks.add(new File(s));
        }
        Collections.sort(singleBooks, NaturalOrderComparator.FILE_COMPARATOR);
        return singleBooks;
    }

    /**
     * Gets the saved collection book files the User chose in {@link de.ph1b.audiobook.activity.FolderChooserActivity}
     *
     * @return An array of chosen collection book folders.
     * @see de.ph1b.audiobook.model.Book.Type#COLLECTION_FILE
     * @see de.ph1b.audiobook.model.Book.Type#COLLECTION_FOLDER
     */
    @NonNull
    private List<File> getCollectionBookFiles() {
        List<String> collectionFoldersStringList = prefs.getCollectionFolders();
        List<File> containingFiles = new ArrayList<>(collectionFoldersStringList.size());
        for (String s : collectionFoldersStringList) {
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                File[] containing = f.listFiles(FileRecognition.FOLDER_AND_MUSIC_FILTER);
                if (containing != null) {
                    containingFiles.addAll(Arrays.asList(containing));
                }
            }
        }
        Collections.sort(containingFiles, NaturalOrderComparator.FILE_COMPARATOR);
        return containingFiles;
    }

    /**
     * Deletes all the books that exist on the database but not on the hard drive or on the saved
     * audio book paths.
     */
    private void deleteOldBooks() throws InterruptedException {
        final String TAG = BookAdder.TAG + "#deleteOldBooks()";
        L.d(TAG, "started");
        List<File> singleBookFiles = getSingleBookFiles();
        List<File> collectionBookFolders = getCollectionBookFiles();

        //getting books to remove
        List<Book> booksToRemove = new ArrayList<>(20);
        for (Book book : db.getActiveBooks()) {
            boolean bookExists = false;
            switch (book.getType()) {
                case COLLECTION_FILE:
                    for (File f : collectionBookFolders) {
                        if (f.isFile()) {
                            List<Chapter> chapters = book.getChapters();
                            File singleBookChapterFile = chapters.get(0).getFile();
                            if (singleBookChapterFile.equals(f)) {
                                bookExists = true;
                            }
                        }
                    }
                    break;
                case COLLECTION_FOLDER:
                    for (File f : collectionBookFolders) {
                        if (f.isDirectory()) { // multi file book
                            if (book.getRoot().equals(f.getAbsolutePath())) {
                                bookExists = true;
                            }
                        }
                    }
                    break;
                case SINGLE_FILE:
                    for (File f : singleBookFiles) {
                        if (f.isFile()) {
                            List<Chapter> chapters = book.getChapters();
                            File singleBookChapterFile = chapters.get(0).getFile();
                            if (singleBookChapterFile.equals(f)) {
                                bookExists = true;
                            }
                        }
                    }
                    break;
                case SINGLE_FOLDER:
                    for (File f : singleBookFiles) {
                        if (f.isDirectory()) { // multi file book
                            if (book.getRoot().equals(f.getAbsolutePath())) {
                                bookExists = true;
                            }
                        }
                    }
                    break;
                default:
                    throw new AssertionError("We added somewhere a non valid type=" + book.getType());
            }

            if (!bookExists) {
                booksToRemove.add(book);
            }
        }

        if (!BaseActivity.storageMounted()) {
            throw new InterruptedException("Storage is not mounted");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (ContextCompat.checkSelfPermission(c, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                throw new InterruptedException("Does not have external storage permission");
            }
        }

        for (Book b : booksToRemove) {
            L.d(TAG, "deleting book=" + b);
            db.hideBook(b);
        }
        L.d(TAG, "finished");
    }

    /**
     * Adds a new book
     *
     * @param rootFile    The root of the book
     * @param newChapters The new chapters that have been found matching to the location of the book
     * @param type        The type of the book
     */
    private void addNewBook(@NonNull File rootFile, @NonNull List<Chapter> newChapters, @NonNull Book.Type type) {
        String bookRoot = rootFile.isDirectory() ?
                rootFile.getAbsolutePath() :
                rootFile.getParent();

        File firstChapterFile = newChapters.get(0).getFile();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String bookName = getBookName(firstChapterFile, rootFile, mmr);
        String author = getAuthor(firstChapterFile, mmr);
        mmr.release();

        final Book orphanedBook = getBookFromDb(rootFile, type, true);
        if (orphanedBook == null) {
            Book newBook = new Book(bookRoot, bookName, author, newChapters,
                    firstChapterFile, type, new ArrayList<Bookmark>(10), c);
            L.d(TAG, "adding newBook=" + newBook);
            db.addBook(newBook);
        } else { // restore old books
            // first adds all chapters
            orphanedBook.getChapters().clear();
            orphanedBook.getChapters().addAll(newChapters);

            // now removes invalid bookmarks
            List<Bookmark> filteredBookmarks = Lists.newArrayList(Collections2.filter(
                    orphanedBook.getBookmarks(), new Predicate<Bookmark>() {
                        @Override
                        public boolean apply(Bookmark input) {
                            for (Chapter c : orphanedBook.getChapters()) {
                                if (c.getFile().equals(input.getMediaFile())) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    }));
            orphanedBook.getBookmarks().clear();
            orphanedBook.getBookmarks().addAll(filteredBookmarks);

            // checks if current path is still valid. if not, reset position.
            boolean pathValid = false;
            for (Chapter c : orphanedBook.getChapters()) {
                if (c.getFile().equals(orphanedBook.getCurrentFile())) {
                    pathValid = true;
                }
            }
            if (!pathValid) {
                orphanedBook.setPosition(0, orphanedBook.getChapters().get(0).getFile());
            }

            // now finally un-hide this book
            db.revealBook(orphanedBook);
        }
    }

    /**
     * Updates a book. Adds the new chapters to the book and corrects the
     * {@link Book#currentFile} and {@link Book#time}.
     *
     * @param bookExisting The existing book
     * @param newChapters  The new chapters matching to the book
     */
    private void updateBook(@NonNull final Book bookExisting, @NonNull List<Chapter> newChapters) {
        boolean bookHasChanged = !(bookExisting.getChapters().equals(newChapters));
        // sort chapters
        if (bookHasChanged) {
            bookExisting.getChapters().clear();
            bookExisting.getChapters().addAll(newChapters);

            // check if the chapter set as the current still exists
            boolean currentPathIsGone = true;
            File currentFile = bookExisting.getCurrentFile();
            int currentTime = bookExisting.getTime();
            for (Chapter c : bookExisting.getChapters()) {
                if (c.getFile().equals(currentFile)) {
                    if (c.getDuration() < currentTime) {
                        bookExisting.setPosition(0, c.getFile());
                    }
                    currentPathIsGone = false;
                }
            }
            if (currentPathIsGone) {
                bookExisting.setPosition(0, bookExisting.getChapters().get(0).getFile());
            }

            // removes the bookmarks that no longer represent an existing file
            List<Bookmark> existingBookmarks = bookExisting.getBookmarks();
            List<Bookmark> filtered = Lists.newArrayList(Collections2.filter(existingBookmarks, new Predicate<Bookmark>() {
                @Override
                public boolean apply(Bookmark input) {
                    for (Chapter c : bookExisting.getChapters()) {
                        if (c.getFile().equals(input.getMediaFile())) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
            existingBookmarks.clear();
            existingBookmarks.addAll(filtered);

            db.updateBook(bookExisting);
        }
    }

    /**
     * Adds a book if not there yet, updates it if there are changes or hides it if it does not
     * exist any longer
     *
     * @param rootFile The Book root
     * @param type     The type of the book
     * @throws InterruptedException If the scanner has been requested to reset
     */
    private void checkBook(@NonNull File rootFile, @NonNull Book.Type type) throws InterruptedException {
        List<Chapter> newChapters = getChaptersByRootFile(rootFile);
        Book bookExisting = getBookFromDb(rootFile, type, false);

        if (!BaseActivity.storageMounted()) {
            throw new InterruptedException("Storage not mounted");
        }

        if (newChapters.isEmpty()) { // there are no chapters
            if (bookExisting != null) {//so delete book if available
                db.hideBook(bookExisting);
            }
        } else { // there are chapters
            if (bookExisting == null) { //there is no active book.
                addNewBook(rootFile, newChapters, type);
            } else { //there is a book, so update it if necessary
                updateBook(bookExisting, newChapters);
            }
        }
    }

    /**
     * Returns all the chapters matching to a Book root
     *
     * @param rootFile The root of the book
     * @return The chapters
     * @throws InterruptedException If the scanner has been requested to terminate
     */
    @NonNull
    private List<Chapter> getChaptersByRootFile(@NonNull File rootFile) throws InterruptedException {
        List<File> containingFiles = getAllContainingFiles(Collections.singletonList(rootFile), true);
        // sort the files in a natural way
        Collections.sort(containingFiles, NaturalOrderComparator.FILE_COMPARATOR);
        L.i(TAG, "Got files=" + containingFiles);

        // get duration and if there is no cover yet, try to get an embedded dover (up to 5 times)
        List<Chapter> containingMedia = new ArrayList<>(containingFiles.size());
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            for (File f : containingFiles) {
                try {
                    mmr.setDataSource(f.getAbsolutePath());

                    // getting chapter-name
                    String chapterName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    // checking for dot index because otherwise a file called ".mp3" would have no name.
                    if (chapterName == null || chapterName.isEmpty()) {
                        String fileName = Files.getNameWithoutExtension(f.getAbsolutePath());
                        chapterName = fileName.isEmpty() ? f.getName() : fileName;
                    }

                    String durationString = mmr.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durationString != null) {
                        int duration = Integer.parseInt(durationString);
                        if (duration > 0) {
                            containingMedia.add(new Chapter(f, chapterName, duration));
                        }
                    }

                    throwIfStopRequested();
                } catch (RuntimeException e) {
                    L.e(TAG, "Error at file=" + f);
                    e.printStackTrace();
                }
            }
        } finally {
            mmr.release();
        }
        return containingMedia;
    }

    /**
     * Throws an interruption if {@link #stopScanner} is true.
     *
     * @throws InterruptedException
     */
    private void throwIfStopRequested() throws InterruptedException {
        if (stopScanner) {
            throw new InterruptedException("Interruption requested");
        }
    }


    /**
     * Gets a book from the database matching to a defines mask.
     *
     * @param rootFile The root of the book
     * @param type     The type of the book
     * @param orphaned If we sould return a book that is orphaned, or a book that is currently
     *                 active
     * @return The Book if available, or {@code null}
     */
    @Nullable
    private Book getBookFromDb(@NonNull File rootFile, @NonNull Book.Type type, boolean orphaned) {
        L.d(TAG, "getBookFromDb, rootFile=" + rootFile + ", type=" + type + ", orphaned=" + orphaned);
        List<Book> books;
        if (orphaned) {
            books = db.getOrphanedBooks();
        } else {
            books = db.getActiveBooks();
        }
        if (rootFile.isDirectory()) {
            for (Book b : books) {
                if (rootFile.getAbsolutePath().equals(b.getRoot()) && type == b.getType()) {
                    return b;
                }
            }
        } else if (rootFile.isFile()) {
            L.d(TAG, "getBookFromDb, its a file");
            for (Book b : books) {
                L.v(TAG, "comparing bookRoot=" + b.getRoot() + " with " + rootFile.getParentFile().getAbsolutePath());
                if (rootFile.getParentFile().getAbsolutePath().equals(b.getRoot()) && type == b.getType()) {
                    Chapter singleChapter = b.getChapters().get(0);
                    L.d(TAG, "getBookFromDb, singleChapterPath=" + singleChapter.getFile() + " compared with=" + rootFile.getAbsolutePath());
                    if (singleChapter.getFile().equals(rootFile)) {
                        return b;
                    }
                }
            }
        }
        return null;
    }
}
