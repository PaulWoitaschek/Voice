package de.ph1b.audiobook.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.NaturalOrderComparator;
import de.ph1b.audiobook.utils.PrefsManager;

public class BookAddingService extends Service {

    public static final FileFilter folderAndMusicFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return isAudio(pathname) || pathname.isDirectory();
        }
    };
    private static final String ACTION_UPDATE_BOOKS = "actionUpdateBooks";
    private static final String TAG = BookAddingService.class.getSimpleName();
    private static final ArrayList<String> audioTypes = new ArrayList<>();
    private static final ArrayList<String> imageTypes = new ArrayList<>();

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
        if (Build.VERSION.SDK_INT >= 21) {
            audioTypes.add(".opus");
        }

        imageTypes.add(".jpg");
        imageTypes.add(".jpeg");
        imageTypes.add(".bmp");
        imageTypes.add(".png");
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ReentrantLock bookLock = new ReentrantLock();
    private BaseApplication baseApplication;
    private ArrayList<Book> allBooks;
    private PrefsManager prefs;
    private DataBaseHelper db;
    private volatile boolean stopScanner = false;

    public static Intent getUpdateIntent(Context c) {
        Intent i = new Intent(c, BookAddingService.class);
        i.setAction(ACTION_UPDATE_BOOKS);
        return i;
    }

    private static boolean isAudio(File f) {
        for (String s : audioTypes) {
            if (f.getName().toLowerCase().endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        baseApplication = (BaseApplication) getApplication();
        allBooks = baseApplication.getAllBooks();
        prefs = new PrefsManager(this);
        db = DataBaseHelper.getInstance(this);
    }

    private void addNewBooks() {
        ExecutorService bookAdder = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                return thread;
            }
        });

        ArrayList<File> containingFiles = getContainingFiles();
        for (final File f : containingFiles) {
            bookAdder.execute(new Runnable() {
                @Override
                public void run() {
                    addNewBook(f);
                }
            });
        }
        try {
            bookAdder.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void scanForFiles() {
        stopScanner = true;
        L.d(TAG, "scan files abort requested");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "started new scanner");
                stopScanner = false;

                deleteOldBooks();
                if (stopScanner) {
                    L.d(TAG, "aborting scanFiles after deleteOldBooks");
                    return;
                }

                addNewBooks();
                if (stopScanner) {
                    L.d(TAG, "aborting scanFiles after addNewBooks");
                    return;
                }

                L.d(TAG, "scanner finished normally");
            }
        });
    }

    private boolean isImage(File f) {
        for (String s : imageTypes) {
            if (f.getName().toLowerCase().endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<File> getContainingFiles() {
        // getting all files who are in the root of the chosen folders
        ArrayList<String> folders = prefs.getAudiobookFolders();
        ArrayList<File> containingFiles = new ArrayList<>();
        for (String s : folders) {
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                containingFiles.addAll(Arrays.asList(f.listFiles(folderAndMusicFilter)));
            }
        }
        return containingFiles;
    }

    private void deleteOldBooks() {
        ArrayList<File> containingFiles = getContainingFiles();

        //getting books to remove
        ArrayList<Book> booksToRemove = new ArrayList<>();
        for (Book book : allBooks) {
            boolean bookExists = false;
            for (File f : containingFiles) {
                if (f.isDirectory()) { // multi file book
                    if (book.getRoot().equals(f.getAbsolutePath())) {
                        bookExists = true;
                    }
                } else if (f.isFile()) { // single file book
                    ArrayList<Chapter> chapters = book.getChapters();
                    String singleBookChapterPath = book.getRoot() + "/" + chapters.get(0).getPath();
                    if (singleBookChapterPath.equals(f.getAbsolutePath())) {
                        bookExists = true;
                    }
                }
            }
            if (!bookExists) {
                booksToRemove.add(book);
            }
        }

        for (Book b : booksToRemove) {
            L.d(TAG, "deleting book=" + b);
            db.deleteBook(b);
            allBooks.remove(b);
            baseApplication.notifyBookDeleted();
        }
    }

    private void addNewBook(File f) {
        Book bookExisting = getBookByRoot(f);
        Book newBook = rootFileToBook(f);

        // this check is important
        if (stopScanner) {
            return;
        }

        // delete old book if it exists and is different from the new book
        if (!stopScanner && (bookExisting != null && (newBook == null || !newBook.equals(bookExisting)))) {
            bookLock.lock();
            try {
                L.d(TAG, "addNewBook deletes existing book=" + bookExisting + " because it is different from newBook=" + newBook);
                db.deleteBook(bookExisting);
                allBooks.remove(bookExisting);
                baseApplication.notifyBookDeleted();
            } finally {
                bookLock.unlock();
            }
        }

        // if there are no changes, we can skip this one
        // skip it if there is no new book or if there is a new and an old book and they are not the same.
        if (newBook == null || (bookExisting != null && bookExisting.equals(newBook))) {
            return;
        }

        if (stopScanner) {
            L.d(TAG, "addNewBook(); stopScanner requested");
            return;
        }

        bookLock.lock();
        try {
            db.addBook(newBook);
            allBooks.add(newBook);
            baseApplication.notifyBookAdded();
        } finally {
            bookLock.unlock();
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
            if (f.exists() && f.isFile())
                fileList.add(f);
            else if (f.exists() && f.isDirectory()) {
                dirList.add(f);
            }
        }
        Collections.sort(fileList, new NaturalOrderComparator());
        returnList.addAll(fileList);
        Collections.sort(dirList, new NaturalOrderComparator());
        for (File f : dirList) {
            ArrayList<File> content = new ArrayList<>(Arrays.asList(f.listFiles()));
            if (content.size() > 0) {
                ArrayList<File> tempReturn = addFilesRecursive(content);
                returnList.addAll(tempReturn);
            }
        }
        return returnList;
    }

    @Nullable
    private Book rootFileToBook(File rootFile) {
        if (stopScanner) {
            return null;
        }

        ArrayList<File> rootFiles = new ArrayList<>();
        rootFiles.add(rootFile);
        rootFiles = addFilesRecursive(rootFiles);
        ArrayList<Chapter> containingMedia = new ArrayList<>();
        ArrayList<File> coverFiles = new ArrayList<>();
        ArrayList<File> musicFiles = new ArrayList<>();
        for (File f : rootFiles) {
            if (isAudio(f)) {
                musicFiles.add(f);
            } else if (isImage(f)) {
                coverFiles.add(f);
            }
        }

        if (musicFiles.size() == 0) {
            L.d(TAG, "assAsBook with file=" + rootFiles + " aborted because it contains no audiofiles");
        }

        Bitmap cover = null;

        // if there are images, get the first one.
        int dimen = ImageHelper.getCoverLength(this);
        for (File f : coverFiles) {
            if (cover == null) {
                try {
                    cover = Picasso.with(this).load(f).resize(dimen, dimen).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String bookRoot;
        String bookName;
        if (rootFile.isDirectory()) {
            bookRoot = rootFile.getAbsolutePath();
            bookName = rootFile.getName();
        } else {
            bookRoot = rootFile.getParent();
            bookName = rootFile.getName().substring(0, rootFile.getName().lastIndexOf("."));
        }

        // get duration and if there is no cover yet, try to get an embedded dover (up to 5 times)
        final int MAX_TRIES_FOR_EMBEDDED_COVER = 5;
        MediaPlayer mp = new MediaPlayer();
        try {
            for (int i = 0; i < musicFiles.size(); i++) {
                File f = musicFiles.get(i);
                int duration = 0;
                try {
                    mp.setDataSource(f.getPath());
                    mp.prepare();
                    duration = mp.getDuration();
                } catch (IOException e) {
                    L.e(TAG, "io error at file f=" + f);
                }
                mp.reset();

                String chapterName = f.getName().substring(0, f.getName().lastIndexOf("."));
                if (duration > 0) {
                    containingMedia.add(new Chapter(f.getAbsolutePath().substring(bookRoot.length() + 1), chapterName, duration));
                }

                if (i < MAX_TRIES_FOR_EMBEDDED_COVER && cover == null) {
                    cover = ImageHelper.getEmbeddedCover(f, this);
                }
                if (stopScanner) {
                    L.d(TAG, "rootFileToBook, stopScanner called");
                    return null;
                }
            }
        } finally {
            mp.release();
        }

        if (containingMedia.size() == 0) {
            L.e(TAG, "Book with root=" + rootFiles + " contains no media");
            return null;
        }


        String coverPath = null;
        if (cover != null) {
            coverPath = ImageHelper.saveCover(cover, this);
        }

        return new Book(bookRoot, bookName, containingMedia, new ArrayList<Bookmark>(), coverPath, 1);
    }

    @Nullable
    private Book getBookByRoot(File rootFile) {
        if (rootFile.isDirectory()) {
            for (Book b : allBooks) {
                if (rootFile.getAbsolutePath().equals(b.getRoot())) {
                    return b;
                }
            }
        } else if (rootFile.isFile()) {
            for (Book b : allBooks) {
                if (rootFile.getParentFile().getAbsolutePath().equals(b.getRoot())) {
                    Chapter singleChapter = b.getChapters().get(0);
                    if ((b.getRoot() + "/" + singleChapter.getPath()).equals(rootFile.getAbsolutePath())) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_UPDATE_BOOKS)) {
            scanForFiles();
        }

        return START_NOT_STICKY;
    }
}
