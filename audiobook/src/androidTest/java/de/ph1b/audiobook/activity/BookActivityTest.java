package de.ph1b.audiobook.activity;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;


public class BookActivityTest extends ActivityInstrumentationTestCase2<BookActivity> {

    private final Random rnd = new Random();

    public BookActivityTest() {
        super(BookActivity.class);
    }

    @MediumTest
    public void testAddingRemovingBooksAsynchronous() throws InterruptedException {

        final ReentrantLock lock = new ReentrantLock();
        final int THREADS = 100;
        final CountDownLatch latch = new CountDownLatch(THREADS);
        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final ArrayList<Book> allBooks = db.getActiveBooks();
        final int BOOKS_AT_ONCE = 3;

        for (int i = 0; i < THREADS; i++) {
            boolean remove = rnd.nextInt(3) > 0; // remove with 1 third prop
            if (remove) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lock.lock();

                        for (int i = 0; i < BOOKS_AT_ONCE; i++) {
                            if (allBooks.size() > 0) {
                                Book book = allBooks.get(rnd.nextInt(allBooks.size()));
                                db.hideBook(book);
                            }
                        }

                        lock.unlock();
                        latch.countDown();
                    }
                }).start();
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lock.lock();

                        for (int i = 0; i < BOOKS_AT_ONCE; i++) {
                            db.addBook(randomBook());
                        }

                        lock.unlock();
                        latch.countDown();
                    }
                }).start();
            }
        }

        latch.await();

    }

    private Book randomBook() {
        ArrayList<Chapter> chapters = new ArrayList<>();
        chapters.add(new Chapter(randomString(), randomString(), rnd.nextInt()));

        return new Book(randomString(), randomString(), randomString(), chapters, chapters.get(0).getPath(),
                Book.Type.COLLECTION_FILE, new ArrayList<Bookmark>(), getActivity());

    }

    private String randomString() {
        String pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String random = "";
        for (int i = 0; i < 5 + rnd.nextInt(5); i++) {
            random += pool.charAt(rnd.nextInt(pool.length()));
        }
        return random;
    }
}
