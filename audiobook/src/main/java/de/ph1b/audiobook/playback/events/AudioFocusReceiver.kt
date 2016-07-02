package de.ph1b.audiobook.playback.events

import android.media.AudioManager
import android.telephony.TelephonyManager
import d
import i
import rx.Observable
import rx.subjects.PublishSubject
import javax.inject.Inject

/**
 * Provides an [AudioManager.OnAudioFocusChangeListener] for registering and an Observable to observe
 * changes in audiofocus. Also notifies when there is an incoming call.
 *
 * @author Paul Woitaschek
 */
class AudioFocusReceiver
@Inject constructor(private val telephonyManager: TelephonyManager) {

    val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        i { "audio focus listener got focus $focusChange" }
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
            d { "Call state is: ${telephonyManager.callState}" }
            subject.onNext(AudioFocus.LOSS_INCOMING_CALL)
        } else {
            i { "FocusChange is $focusChange" }
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> subject.onNext(AudioFocus.GAIN)
                AudioManager.AUDIOFOCUS_LOSS -> subject.onNext(AudioFocus.LOSS)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> subject.onNext(AudioFocus.LOSS_TRANSIENT_CAN_DUCK)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> subject.onNext(AudioFocus.LOSS_TRANSIENT)
            }
        }
    }

    fun focusObservable(): Observable<AudioFocus> = subject.asObservable()

    private val subject = PublishSubject.create<AudioFocus>()
}