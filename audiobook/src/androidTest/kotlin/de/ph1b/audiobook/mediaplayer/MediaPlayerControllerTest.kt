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

package de.ph1b.audiobook.mediaplayer

import android.test.ApplicationTestCase
import android.test.suitebuilder.annotation.MediumTest
import android.test.suitebuilder.annotation.SmallTest
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.testing.DummyCreator
import de.ph1b.audiobook.testing.RealFileMocker
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * Simple test for our MediaPlayer.

 * @author Paul Woitaschek
 */
class MediaPlayerControllerTest : ApplicationTestCase<App> (App::class.java) {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playStateManager: PlayStateManager
    private lateinit var realFileMocker: RealFileMocker
    private lateinit var files: List<File>

    lateinit var book: Book

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        createApplication()
        mediaPlayer = App.component().mediaPlayerController()
        playStateManager = App.component().playStateManager()


        realFileMocker = RealFileMocker()
        files = realFileMocker.create(context);


        book = DummyCreator.dummyBook(files[0], files[1])

        mediaPlayer.init(book)
    }

    override fun testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly()

        checkNotNull(mediaPlayer)
        for (f in files) {
            check(f.exists())
        }
    }

    /**
     * Tests simple play pause controls
     */
    @SmallTest
    fun testSimplePlayback() {
        mediaPlayer.play()
        check(playStateManager.playState.value == PlayStateManager.PlayState.PLAYING)
        Thread.sleep(1000)
        mediaPlayer.pause(false)
        check(playStateManager.playState.value == PlayStateManager.PlayState.PAUSED)
    }

    private val rnd = Random()

    private fun playPauseRandom() {
        synchronized(mediaPlayer, {
            if (rnd.nextBoolean()) {
                mediaPlayer.play()
                check(playStateManager.playState.value == PlayStateManager.PlayState.PLAYING)
            } else {
                mediaPlayer.pause(false)
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
        realFileMocker.destroy()
        super.tearDown()
    }
}
