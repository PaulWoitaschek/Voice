package de.ph1b.audiobook.mediaplayer

import android.os.Environment
import android.test.AndroidTestCase
import android.test.suitebuilder.annotation.MediumTest
import android.test.suitebuilder.annotation.SmallTest
import com.google.common.io.ByteStreams
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.testing.DummyCreator
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

/**
 * Simple test for our MediaPlayer.

 * @author Paul Woitaschek
 */
class MediaPlayerControllerTest : AndroidTestCase () {

    @Inject internal lateinit var mediaPlayerController: MediaPlayerController
    @Inject internal lateinit var playStateManager: PlayStateManager
    lateinit var file1: File
    lateinit var file2: File
    lateinit var book: Book

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        App.component().inject(this)

        val externalStorage = Environment.getExternalStorageDirectory()

        file1 = File(externalStorage, "1.mp3")
        file2 = File(externalStorage, "2.mp3")

        ByteStreams.copy(context.assets.open("3rdState.mp3"), FileOutputStream(file1))
        ByteStreams.copy(context.assets.open("Crashed.mp3"), FileOutputStream(file2))

        book = DummyCreator.dummyBook(file1, file2)

        mediaPlayerController.init(book)
    }

    override fun testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly()

        checkNotNull(mediaPlayerController)
        check(file1.exists())
        check(file2.exists())
    }

    /**
     * Tests simple play pause controls
     */
    @SmallTest
    fun testSimplePlayback() {
        mediaPlayerController.play()
        check(playStateManager.playState.value == PlayStateManager.PlayState.PLAYING)
        Thread.sleep(1000)
        mediaPlayerController.pause(false)
        check(playStateManager.playState.value == PlayStateManager.PlayState.PAUSED)
    }

    private val rnd = Random()

    private fun playPauseRandom() {
        synchronized(mediaPlayerController, {
            if (rnd.nextBoolean()) {
                mediaPlayerController.play()
                check(playStateManager.playState.value == PlayStateManager.PlayState.PLAYING)
            } else {
                mediaPlayerController.pause(false)
                check(playStateManager.playState.value == PlayStateManager.PlayState.PAUSED)
            }
        })
    }

    /**
     * Tests for threading issues by letting two threads against each other
     */
    @MediumTest
    fun testThreading() {
        val commandsToExecute = 1..1000
        val readyLatch = CountDownLatch(2)
        val t1 = Thread(Runnable {
            readyLatch.countDown()
            readyLatch.await()

            for (i in commandsToExecute) {
                playPauseRandom()
            }
        })
        val t2 = Thread(Runnable {
            readyLatch.countDown()
            readyLatch.await()

            for (i in commandsToExecute) {
                playPauseRandom()
            }
        })

        t1.join()
        t2.join()
    }


    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()

        file1.delete()
        file2.delete()
    }
}
