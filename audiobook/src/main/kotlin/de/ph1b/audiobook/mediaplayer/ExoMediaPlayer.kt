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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.mediaplayer

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.ExoPlayer
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultUriDataSource
import de.ph1b.audiobook.playback.SpeedRenderer
import rx.Observable
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Convenient wrapper around [ExoPlayer]
 *
 * @author Paul Woitaschek
 */
class ExoMediaPlayer
@Inject
constructor(val context: Context) {

    private val exoPlayer = ExoPlayer.Factory.newInstance(1);
    private val wakeLock: PowerManager.WakeLock

    private val BUFFER_SEGMENT_SIZE = 64 * 1024;
    private val BUFFER_SEGMENT_COUNT = 256;

    private var audioRenderer: SpeedRenderer? = null

    init {
        exoPlayer.addListener(object : ExoPlayer.Listener {
            override fun onPlayerError(error: ExoPlaybackException?) {
                errorSubject.onNext(Unit)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Timber.i("onPlayStateChanged with playWhenReady=$playWhenReady and playState=$playbackState");
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    completionSubject.onNext(Unit)
                }
            }

            override fun onPlayWhenReadyCommitted() {

            }
        })

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "exoPlayer");
        wakeLock.setReferenceCounted(false);
    }


    /**
     * Prepares an audio file.
     */
    fun prepare(file: File) {
        val uri = Uri.fromFile(file)
        val userAgent = "MaterialAudiobookPlayer"
        val allocator = DefaultAllocator(BUFFER_SEGMENT_SIZE)
        val dataSource = DefaultUriDataSource(context, null, userAgent);
        val sampleSource = ExtractorSampleSource(uri, dataSource, allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        audioRenderer = SpeedRenderer(sampleSource);
        audioRenderer!!.playbackSpeed = playbackSpeed
        exoPlayer.prepare(audioRenderer);
    }

    /**
     * The current position in the track.
     */
    var currentPosition: Int
        get() = exoPlayer.currentPosition.toInt()
        set(value) {
            exoPlayer.seekTo(value.toLong())
        }

    /**
     * If true the player will start as soon as he is prepared
     */
    var autoPlay: Boolean
        set(autoPlay) {
            wakeLock.apply {
                if (autoPlay && isHeld.not()) {
                    acquire()
                } else if (!autoPlay && isHeld) {
                    release()
                }
            }

            exoPlayer.playWhenReady = autoPlay
        }
        get() = exoPlayer.playWhenReady

    /**
     * The playback rate. 1.0 is normal
     */
    var playbackSpeed: Float = 1F
        set(value) {
            audioRenderer?.playbackSpeed = value
            field = value
        }

    private val errorSubject = PublishSubject.create<Unit>()

    /**
     * An observable that emits when an error is detected
     */
    val errorObservable: Observable<Unit> = errorSubject.asObservable()

    private val completionSubject = PublishSubject.create<Unit>()

    /**
     * An observable that emits once a track is finished.
     */
    val completionObservable: Observable<Unit> = completionSubject.asObservable()

    /**
     * The duration of the current track
     */
    val duration: Int
        get() = exoPlayer.duration.toInt()
}
