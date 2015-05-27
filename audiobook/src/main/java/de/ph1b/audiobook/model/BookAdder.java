package de.ph1b.audiobook.model;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookAdder {

    private static final String TAG = BookAdder.class.getSimpleName();
    private static final ArrayList<String> audioTypes = new ArrayList<>();
    public static final FileFilter folderAndMusicFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return isAudio(pathname) || pathname.isDirectory();
        }
    };
    private static final ArrayList<String> imageTypes = new ArrayList<>();
    private static final FileFilter imageFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            for (String s : imageTypes) {
                if (pathname.getAbsolutePath().toLowerCase().endsWith(s))
                    return true;
            }
            return false;
        }
    };
    public static volatile boolean scannerActive = false;
    private static BookAdder instance;

    static {
        audioTypes.add(".3gp");
        audioTypes.add(".mp4");
        audioTypes.add(".m4a");
        audioTypes.add(".m4b");
        audioTypes.add(".mp3");
        audioTypes.add(".mid");
        audioTypes.add(".xmf");
        audioTypes.add(".mxmf");
        audioTypes.add(".rtttl");
        audioTypes.add(".rtx");
        audioTypes.add(".ota");
        audioTypes.add(".imy");
        audioTypes.add(".ogg");
        audioTypes.add(".oga");
        audioTypes.add(".wav");
        audioTypes.add(".aac");
        audioTypes.add(".flac");
        audioTypes.add(".mkv");
        audioTypes.add(".wma");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTypes.add(".opus");
        }

        imageTypes.add(".jpg");
        imageTypes.add(".jpeg");
        imageTypes.add(".bmp");
        imageTypes.add(".png");
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context c;
    private final PrefsManager prefs;
    private final DataBaseHelper db;
    private volatile boolean stopScanner = false;

    private BookAdder(@NonNull Context c) {
        this.c = c;
        prefs = new PrefsManager(this.c);
        db = DataBaseHelper.getInstance(this.c);
    }

    public static synchronized BookAdder getInstance(Context c) {
        if (instance == null) {
            instance = new BookAdder(c.getApplicationContext());
        }
        return instance;
    }

    private static boolean isAudio(File f) {
        for (String s : audioTypes) {
            if (f.getName().toLowerCase().endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private void addNewBooks() throws InterruptedException {
        ArrayList<File> singleBooks = getSingleBookFiles();
        for (File f : singleBooks) {
            L.d(TAG, "addNewBooks with singleBookFile=" + f);
            if (f.isFile() && f.canRead()) {
                addNewBook(f, Book.Type.SINGLE_FILE);
            } else if (f.isDirectory() && f.canRead()) {
                addNewBook(f, Book.Type.SINGLE_FOLDER);
            }
        }

        ArrayList<File> collectionBooks = getCollectionBookFiles();
        for (File f : collectionBooks) {
            L.d(TAG, "checking collectionBook=" + f);
            if (f.isFile() && f.canRead()) {
                addNewBook(f, Book.Type.COLLECTION_FILE);
            } else if (f.isDirectory() && f.canRead()) {
                addNewBook(f, Book.Type.COLLECTION_FOLDER);
            }
        }
    }

    @Nullable
    private Bitmap getCoverFromDisk(@NonNull File[] coverFiles) throws InterruptedException {
        // if there are images, get the first one.
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        int dimen = ImageHelper.getSmallerScreenSize(c);
        for (File f : coverFiles) {
            if (stopScanner) throw new InterruptedException("Interrupted at getCoverFromDisk");
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

    @Nullable
    private Bitmap getEmbeddedCover(@NonNull ArrayList<Chapter> chapters) throws InterruptedException {
        int tries = 0;
        int maxTries = 5;
        for (Chapter c : chapters) {
            if (++tries < maxTries) {
                if (stopScanner) throw new InterruptedException("Interrupted at getEmbeddedCover");
                Bitmap cover = ImageHelper.getEmbeddedCover(new File(c.getPath()), this.c);
                if (cover != null)
                    return cover;
            } else {
                return null;
            }
        }
        return null;
    }

    private void findCovers() throws InterruptedException {
        for (Book b : db.getActiveBooks()) {
            if (stopScanner) throw new InterruptedException("interrupted at findCover");
            File coverFile = b.getCoverFile();
            if (!coverFile.exists()) {
                if (b.getType() == Book.Type.COLLECTION_FOLDER || b.getType() == Book.Type.SINGLE_FOLDER) {
                    File root = new File(b.getRoot());
                    if (root.exists()) {
                        File[] images = root.listFiles(imageFilter);
                        if (images != null) {
                            Bitmap cover = getCoverFromDisk(images);
                            if (cover != null) {
                                ImageHelper.saveCover(cover, c, coverFile);
                                Picasso.with(c).invalidate(coverFile);
                                Communication.sendCoverChanged(c, b.getId());
                                continue;
                            }
                        }
                    }
                }
                Bitmap cover = getEmbeddedCover(b.getChapters());
                if (cover != null) {
                    ImageHelper.saveCover(cover, c, coverFile);
                    Picasso.with(c).invalidate(coverFile);
                    Communication.sendCoverChanged(c, b.getId());
                }
            }
        }
    }

    public void scanForFiles(boolean interrupting) {
        L.d(TAG, "scanForFiles called. scannerActive=" + scannerActive + ", interrupting=" + interrupting);
        if (!scannerActive || interrupting) {
            stopScanner = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    L.v(TAG, "started");
                    scannerActive = true;
                    Communication.sendScannerStateChanged(c);
                    stopScanner = false;

                    try {
                        deleteOldBooks();
                        addNewBooks();
                        findCovers();
                    } catch (InterruptedException e) {
                        L.d(TAG, "We were interrupted at adding a book", e);
                    }

                    stopScanner = false;
                    scannerActive = false;
                    Communication.sendScannerStateChanged(c);
                    L.v(TAG, "stopped");
                }
            });
        }
        L.v(TAG, "scanforfiles method done (executor should be called");
    }

    private ArrayList<File> getSingleBookFiles() {
        ArrayList<File> singleBooks = new ArrayList<>();
        for (String s : prefs.getSingleBookFolders()) {
            singleBooks.add(new File(s));
        }
        Collections.sort(singleBooks, new NaturalOrderComparator());
        return singleBooks;
    }


    private ArrayList<File> getCollectionBookFiles() {
        ArrayList<File> containingFiles = new ArrayList<>();
        for (String s : prefs.getCollectionFolders()) {
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                File[] containing = f.listFiles(folderAndMusicFilter);
                if (containing != null) {
                    containingFiles.addAll(Arrays.asList(containing));
                }
            }
        }
        Collections.sort(containingFiles, new NaturalOrderComparator());
        return containingFiles;
    }


    /**
     * Deletes all the books that exist on the database but not on the hard drive or on the saved
     * audio book paths.
     */
    private void deleteOldBooks() throws InterruptedException {
        final String TAG = BookAdder.TAG + "#deleteOldBooks()";
        L.d(TAG, "started");
        ArrayList<File> singleBookFiles = getSingleBookFiles();
        ArrayList<File> collectionBookFolders = getCollectionBookFiles();

        //getting books to remove
        ArrayList<Book> booksToRemove = new ArrayList<>();
        for (Book book : db.getActiveBooks()) {
            boolean bookExists = false;
            switch (book.getType()) {
                case COLLECTION_FILE:
                    for (File f : collectionBookFolders) {
                        if (f.isFile()) {
                            ArrayList<Chapter> chapters = book.getChapters();
                            String singleBookChapterPath = chapters.get(0).getPath();
                            if (singleBookChapterPath.equals(f.getAbsolutePath())) {
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
                            ArrayList<Chapter> chapters = book.getChapters();
                            String singleBookChapterPath = chapters.get(0).getPath();
                            if (singleBookChapterPath.equals(f.getAbsolutePath())) {
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

        if (!BaseActivity.storageMounted())
            throw new InterruptedException("Storage is not mounted");
        for (Book b : booksToRemove) {
            L.d(TAG, "deleting book=" + b);
            db.hideBook(b);
        }
        L.d(TAG, "finished");
    }


    /**
     * Returns the name of the book we want to add. If there is a tag embedded, use that one. Else
     * derive the title from the filename.
     *
     * @param firstChapterPath A path to a file
     * @param rootFile         The root of the book to add
     * @return The name of the book we add
     */
    @NonNull
    private String getBookName(String firstChapterPath, File rootFile, MediaMetadataRetriever mmr) {
        String bookName = null;
        try {
            mmr.setDataSource(firstChapterPath);
            bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        } catch (RuntimeException ignored) {
        }
        if (bookName == null || bookName.length() == 0) {
            bookName = rootFile.isDirectory() ?
                    rootFile.getName() :
                    rootFile.getName().substring(0, rootFile.getName().lastIndexOf("."));
        }
        return bookName;
    }

    /**
     * Returns the author of the book we want to add. If there is a tag embedded, use that one. Else
     * return null
     *
     * @param firstChapterPath A path to a file
     * @return The name of the book we add
     */
    @Nullable
    private String getAuthor(@NonNull String firstChapterPath, @NonNull MediaMetadataRetriever mmr) {
        try {
            mmr.setDataSource(firstChapterPath);
            String bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            if (bookName == null || bookName.length() == 0) {
                bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
            }
            if (bookName == null || bookName.length() == 0) {
                bookName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            }
            return bookName;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void addNewBook(File rootFile, Book.Type type) throws InterruptedException {
        ArrayList<Chapter> newChapters = getChaptersByRootFile(rootFile);
        Book bookExisting = getBookFromDb(rootFile, type, false);

        if (!BaseActivity.storageMounted())
            throw new InterruptedException("Storage not mounted");

        if (newChapters.size() == 0) { // there are no chapters
            if (bookExisting != null) {//so delete book if available
                db.hideBook(bookExisting);
            }
        } else { // there are chapters
            if (bookExisting == null) { //there is no active book.
                String bookRoot = rootFile.isDirectory() ?
                        rootFile.getAbsolutePath() :
                        rootFile.getParent();

                String firstChapterPath = newChapters.get(0).getPath();
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                String bookName = getBookName(firstChapterPath, rootFile, mmr);
                String author = getAuthor(firstChapterPath, mmr);
                mmr.release();

                Book orphanedBook = getBookFromDb(rootFile, type, true);
                if (orphanedBook == null) {
                    Book newBook = new Book(bookRoot, bookName, author, newChapters,
                            firstChapterPath, type, new ArrayList<Bookmark>(), c);
                    L.d(TAG, "adding newBook=" + newBook);
                    db.addBook(newBook);
                } else { // restore old books
                    // first adds all chapters
                    orphanedBook.getChapters().clear();
                    orphanedBook.getChapters().addAll(newChapters);

                    // now removes invalid bookmarks
                    ArrayList<Bookmark> invalidBookmarks = new ArrayList<>();
                    for (Bookmark bookmark : orphanedBook.getBookmarks()) {
                        boolean bookmarkValid = false;
                        for (Chapter c : orphanedBook.getChapters()) {
                            if (c.getPath().equals(bookmark.getMediaPath()))
                                bookmarkValid = true;
                        }
                        if (!bookmarkValid)
                            invalidBookmarks.add(bookmark);
                    }
                    for (Bookmark invalid : invalidBookmarks) {
                        orphanedBook.getBookmarks().remove(invalid);
                    }

                    // checks if current path is still valid. if not, reset position.
                    boolean pathValid = false;
                    for (Chapter c : orphanedBook.getChapters()) {
                        if (c.getPath().equals(orphanedBook.getCurrentMediaPath()))
                            pathValid = true;
                    }
                    if (!pathValid) {
                        orphanedBook.setPosition(0, orphanedBook.getChapters().get(0).getPath());
                    }

                    // now finally un-hide this book
                    db.reveilBook(orphanedBook);
                }
            } else { //there is a book, so update it if necessary
                boolean bookHasChanged = false;
                ArrayList<Chapter> existingChapters = bookExisting.getChapters();

                // 1. Delete chapters that have the same path, but a different duration
                // 2. Delete chapters that do no longer exist
                Iterator<Chapter> chapterIterator = existingChapters.iterator();
                while (chapterIterator.hasNext()) {
                    Chapter e = chapterIterator.next();
                    boolean deleteChapter = true;
                    for (Chapter n : newChapters) {
                        if (n.getPath().equals(e.getPath()) && n.getDuration() == e.getDuration()) {
                            deleteChapter = false;
                        }
                    }
                    if (deleteChapter) {
                        chapterIterator.remove();
                        bookHasChanged = true;
                    }
                }
                for (Chapter n : existingChapters) {
                    if (!existingChapters.contains(n)) {
                        existingChapters.add(n);
                        bookHasChanged = true;
                    }
                }
                Collections.sort(existingChapters, new NaturalOrderComparator());
                if (bookHasChanged) {
                    if (existingChapters.size() > 0) {
                        boolean currentPathIsGone = true;
                        String currentPath = bookExisting.getCurrentMediaPath();
                        for (Chapter c : existingChapters) {
                            if (c.getPath().equals(currentPath)) {
                                currentPathIsGone = false;
                            }
                        }
                        if (currentPathIsGone) {
                            bookExisting.setPosition(0, existingChapters.get(0).getPath());
                        }
                        db.updateBook(bookExisting);
                    } else {
                        db.hideBook(bookExisting);
                    }
                }
            }
        }
    }

    /**
     * Adds files recursively. First takes all files and adds them sorted to the return list. Then
     * sorts the folders, and then adds their content sorted to the return list.
     *
     * @param dir The dirs and files to be added
     * @return All the files containing in a natural sorted order.
     */
    private ArrayList<File> addFilesRecursive(ArrayList<File> dir) {
        ArrayList<File> returnList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();
        ArrayList<File> dirList = new ArrayList<>();
        for (File f : dir) {
            if (f.exists() && f.isFile()) {
                fileList.add(f);
            } else if (f.exists() && f.isDirectory()) {
                dirList.add(f);
            }
        }
        Collections.sort(fileList, new NaturalOrderComparator());
        returnList.addAll(fileList);
        Collections.sort(dirList, new NaturalOrderComparator());
        for (File f : dirList) {
            ArrayList<File> content = new ArrayList<>();
            File[] containing = f.listFiles();
            if (containing != null) {
                content = new ArrayList<>(Arrays.asList(containing));
            }
            if (content.size() > 0) {
                ArrayList<File> tempReturn = addFilesRecursive(content);
                returnList.addAll(tempReturn);
            }
        }
        return returnList;
    }

    @NonNull
    private ArrayList<Chapter> getChaptersByRootFile(File rootFile) throws InterruptedException {
        ArrayList<File> containingFiles = new ArrayList<>();
        containingFiles.add(rootFile);
        containingFiles = addFilesRecursive(containingFiles);

        ArrayList<File> musicFiles = new ArrayList<>();
        for (File f : containingFiles) {
            if (isAudio(f)) {
                musicFiles.add(f);
            }
        }

        // get duration and if there is no cover yet, try to get an embedded dover (up to 5 times)
        ArrayList<Chapter> containingMedia = new ArrayList<>();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            for (int i = 0; i < musicFiles.size(); i++) {
                File f = musicFiles.get(i);
                try {
                    mmr.setDataSource(f.getAbsolutePath());

                    // getting chapter-name
                    String chapterName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    // checking for dot index because otherwise a file called ".mp3" would have no name.
                    if (chapterName == null || chapterName.length() == 0) {
                        String fileName = f.getName();
                        int dotIndex = fileName.lastIndexOf(".");
                        if (dotIndex > 0) {
                            chapterName = fileName.substring(0, dotIndex);
                        } else {
                            chapterName = fileName;
                        }
                    }

                    String durationString = mmr.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durationString == null) {
                        continue;
                    } else {
                        int duration = Integer.parseInt(durationString);
                        if (duration > 0) {
                            containingMedia.add(new Chapter(f.getAbsolutePath(), chapterName, duration));
                        }
                    }
                    if (stopScanner) {
                        throw new InterruptedException("getChaptersByRootFile interrupted");
                    }
                } catch (RuntimeException ignored) {
                }
            }
        } finally {
            mmr.release();
        }
        return containingMedia;
    }


    @Nullable
    private Book getBookFromDb(File rootFile, Book.Type type, boolean orphaned) {
        L.d(TAG, "getBookFromDb, rootFile=" + rootFile + ", type=" + type + ", orphaned=" + orphaned);
        ArrayList<Book> books;
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
                    L.d(TAG, "getBookFromDb, singleChapterPath=" + singleChapter.getPath() + " compared with=" + rootFile.getAbsolutePath());
                    if (singleChapter.getPath().equals(rootFile.getAbsolutePath())) {
                        return b;
                    }
                }
            }
        }
        return null;
    }
}
