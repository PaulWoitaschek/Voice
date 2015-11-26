package de.ph1b.audiobook.utils

import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Class for communicating on different events through [LocalBroadcastManager].
 */
@Singleton
class Communication
@Inject
constructor() {

    private val listeners = ArrayList<BookCommunication>(10)
    private val executor = Executors.newSingleThreadScheduledExecutor()


    /**
     * Notifies the listeners that the sleep-timer has either been started or cancelled.

     * @see MediaPlayerController.sleepSandActive
     */
    @Synchronized fun sleepStateChanged() {
        executor.execute {
            for (listener in listeners) {
                listener.onSleepStateChanged()
            }
        }
    }

    @Synchronized fun removeBookCommunicationListener(listener: BookCommunication) {
        listeners.remove(listener)
        Timber.d("removed listener. Now there are %d", listeners.size)
    }

    @Synchronized fun addBookCommunicationListener(listener: BookCommunication) {
        listeners.add(listener)
        Timber.d("added listener. Now there are %d", listeners.size)
    }

    interface BookCommunication {

        fun onSleepStateChanged()
    }

    open class SimpleBookCommunication : BookCommunication {

        override fun onSleepStateChanged() {

        }
    }
}
