package de.ph1b.audiobook.receiver

import android.media.AudioManager
import android.telephony.TelephonyManager
import rx.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

/**
 * Provides an [AudioManager.OnAudioFocusChangeListener] for registering and an Observable to observe
 * changes in audiofocus. Also notifies when there is an incoming call.
 *
 * @author Paul Woitaschek
 */
class AudioFocusReceiver
@Inject constructor(private val telephonyManager: TelephonyManager) {

    public val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
            Timber.d("Call state is: ${telephonyManager.callState}")
            subject.onNext(AudioFocus.LOSS_INCOMING_CALL)
        } else {
            Timber.i("FocusChange is $focusChange")
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> subject.onNext(AudioFocus.GAIN)
                AudioManager.AUDIOFOCUS_LOSS -> subject.onNext(AudioFocus.LOSS)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> subject.onNext(AudioFocus.LOSS_TRANSIENT_CAN_DUCK)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> subject.onNext(AudioFocus.LOSS_TRANSIENT)
            }
        }
    }

    fun focusObservable() = subject.asObservable()

    private val subject = PublishSubject.create<AudioFocus>()

}