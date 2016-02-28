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

package de.ph1b.audiobook.playback

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class for controlling the player through the service
 */
@Singleton class PlayerController
@Inject constructor(val context: Context) {

    private val playPauseIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    private val playIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_PLAY)
    private val pauseIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_PAUSE)
    private val stopIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_STOP)
    private val nextIntent = intent(ACTION_FORCE_NEXT)
    private val previousIntent = intent(ACTION_FORCE_PREVIOUS)
    private val rewindIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_REWIND)
    private val fastForwardIntent = keyEventIntent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
    private val pauseIntentNonRewinding = intent(ACTION_PAUSE_NON_REWINDING)

    private fun intent(action: String) = Intent(context, BookReaderService::class.java).apply {
        setAction(action)
    }

    private fun keyEventIntent(keyCode: Int) = intent(Intent.ACTION_MEDIA_BUTTON).apply {
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
    }

    fun pause() {
        fire(pauseIntent)
    }

    fun stop() {
        fire(stopIntent)
    }

    fun pauseNonRewinding() {
        fire(pauseIntentNonRewinding)
    }

    fun rewind() {
        fire(rewindIntent)
    }

    fun play() {
        fire(playIntent)
    }

    fun fastForward() {
        fire(fastForwardIntent)
    }

    private fun fire(intent: Intent) {
        context.startService(intent)
    }

    fun previous() {
        fire(previousIntent)
    }

    fun playPause() {
        fire(playPauseIntent)
    }

    fun next() {
        fire(nextIntent)
    }

    fun setSpeed(speed: Float) {
        fire(intent(ACTION_SPEED).apply {
            putExtra(EXTRA_SPEED, speed)
        })
    }

    fun changePosition(time: Int, file: File) {
        fire(intent(ACTION_CHANGE).apply {
            putExtra(CHANGE_TIME, time)
            putExtra(CHANGE_FILE, file.absolutePath)
        })
    }

    companion object {
        val ACTION_SPEED = "ActionsetSpeed"
        val EXTRA_SPEED = "speedExtra"

        val ACTION_CHANGE = "actionChange"
        val CHANGE_TIME = "changeTime"
        val CHANGE_FILE = "changeFile"

        val ACTION_FORCE_NEXT = "actionForceNext"
        val ACTION_FORCE_PREVIOUS = "actionForcePrevious"

        val ACTION_PAUSE_NON_REWINDING = "ActionPauseNonRewinding"
    }
}