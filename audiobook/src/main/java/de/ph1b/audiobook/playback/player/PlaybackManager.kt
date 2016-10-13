package de.ph1b.audiobook.playback.player

/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer

import android.os.Bundle
import android.os.Handler

import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import de.ph1b.audiobook.R
import java.util.concurrent.TimeUnit


class PlaybackManager : Activity() {
    private var b1: Button? = null
    private var b2: Button? = null
    private var b3: Button? = null
    private var b4: Button? = null
    private var iv: ImageView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var startTime = 0.0
    private var finalTime = 0.0
    private val myHandler = Handler()
    private val forwardTime = 5000
    private val backwardTime = 5000
    private var seekbar: SeekBar? = null
    private var tx1: TextView? = null
    private var tx2: TextView? = null
    private var tx3: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.abc_action_menu_layout)

        b1 = findViewById(R.id.button) as Button
        b2 = findViewById(R.id.button) as Button
        b3 = findViewById(R.id.button) as Button
        b4 = findViewById(R.id.button) as Button
        iv = findViewById(R.id.imageView) as ImageView

        tx1 = findViewById(R.id.text) as TextView
        tx2 = findViewById(R.id.text) as TextView
        tx3 = findViewById(R.id.text) as TextView
        tx3!!.text = "Song.mp3"

        mediaPlayer = MediaPlayer.create(this, null)
        seekbar = findViewById(R.id.seekBar) as SeekBar
        seekbar!!.isClickable = false
        b2!!.isEnabled = false

        b3!!.setOnClickListener {
            Toast.makeText(applicationContext, "Playing sound", Toast.LENGTH_SHORT).show()
            mediaPlayer!!.start()

            finalTime = mediaPlayer!!.duration.toDouble()
            startTime = mediaPlayer!!.currentPosition.toDouble()

            if (oneTimeOnly == 0) {
                seekbar!!.max = finalTime.toInt()
                oneTimeOnly = 1
            }
            tx2!!.text = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))

            tx1!!.text = String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))

            seekbar!!.progress = startTime.toInt()
            myHandler.postDelayed(UpdateSongTime, 100)
            b2!!.isEnabled = true
            b3!!.isEnabled = false
        }

        b2!!.setOnClickListener {
            Toast.makeText(applicationContext, "Pausing sound", Toast.LENGTH_SHORT).show()
            mediaPlayer!!.pause()
            b2!!.isEnabled = false
            b3!!.isEnabled = true
        }

        b1!!.setOnClickListener {
            val temp = startTime.toInt()

            if (temp + forwardTime <= finalTime) {
                startTime = startTime + forwardTime
                mediaPlayer!!.seekTo(startTime.toInt())
                Toast.makeText(applicationContext, "You have Jumped forward 5 seconds", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show()
            }
        }

        b4!!.setOnClickListener {
            val temp = startTime.toInt()

            if (temp - backwardTime > 0) {
                startTime = startTime - backwardTime
                mediaPlayer!!.seekTo(startTime.toInt())
                Toast.makeText(applicationContext, "You have Jumped backward 5 seconds", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val UpdateSongTime = object : Runnable {
        override fun run() {
            startTime = mediaPlayer!!.currentPosition.toDouble()
            tx1!!.text = String.format("%d min, %d sec",

                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
            seekbar!!.progress = startTime.toInt()
            myHandler.postDelayed(this, 100)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val id = item.itemId

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_settings) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        var oneTimeOnly = 0
    }
}