package de.ph1b.audiobook.activity;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.DataBaseHelper;

/**
 * Tests basic playback controls.
 *
 * @author Paul Woitaschek
 */
public class MediaPlayerControllerTest extends AndroidTestCase {


    public void testBasicControls() throws InterruptedException {
        final MediaPlayerController mediaPlayerController = new MediaPlayerController(getContext());

        DataBaseHelper db = DataBaseHelper.getInstance(getContext());
        List<Book> books = db.getActiveBooks();
        Book firstBook = books.get(0);

        mediaPlayerController.init(firstBook);

        final CountDownLatch latch = new CountDownLatch(3);

        mediaPlayerController.play();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    synchronized (mediaPlayerController) {
                        mediaPlayerController.play();
                        Assert.assertTrue(MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING);
                    }
                }
                latch.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    synchronized (mediaPlayerController) {
                        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
                            mediaPlayerController.pause(false);
                            Assert.assertTrue(MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PAUSED);
                        }
                    }
                }
                latch.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    synchronized (mediaPlayerController) {
                        mediaPlayerController.stop();
                        Assert.assertTrue(MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.STOPPED);
                    }
                }
                latch.countDown();
            }
        }).start();

        latch.await();
    }
}
