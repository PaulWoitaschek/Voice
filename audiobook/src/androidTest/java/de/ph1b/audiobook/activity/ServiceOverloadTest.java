package de.ph1b.audiobook.activity;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.service.ServiceController;

/**
 * A simple test that sends a large amount of controls to the media player.
 *
 * @author Paul Woitaschek
 */
public class ServiceOverloadTest extends AndroidTestCase {

    private static final Random rnd = new Random();

    public void testServiceOverload() throws InterruptedException {


        final ServiceController controller = new ServiceController(getContext());

        final CountDownLatch latch = new CountDownLatch(5);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    controller.fastForward();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    controller.next();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    controller.playPause();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                latch.countDown();
            }
        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    controller.rewind();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                latch.countDown();
            }
        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    controller.setPlaybackSpeed(1.0f + (rnd.nextBoolean() ? (-0.3f) : +0.3f));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                latch.countDown();
            }
        }).start();


        latch.await();

        MediaPlayerController.PlayState oldState = MediaPlayerController.getPlayState();
        controller.playPause();

        Thread.sleep(5000);
        Assert.assertTrue(MediaPlayerController.getPlayState() != oldState);
    }
}
