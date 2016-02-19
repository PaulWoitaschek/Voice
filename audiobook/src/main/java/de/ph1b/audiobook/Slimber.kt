import android.util.Log
import timber.log.Timber

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

inline fun ifPlanted(action: () -> Any) {
    if (Timber.treeCount() != 0) {
        action.invoke()
    }
}

inline fun e(message: () -> String) = ifPlanted { Timber.e(message.invoke()) }
inline fun e(throwable: Throwable, message: () -> String) = ifPlanted { Timber.e(throwable, message.invoke()) }

inline fun w(message: () -> String) = ifPlanted { Timber.w(message.invoke()) }
inline fun w(throwable: Throwable, message: () -> String) = ifPlanted { Timber.w(throwable, message.invoke()) }

inline fun i(message: () -> String) = ifPlanted { Timber.i(message.invoke()) }
inline fun i(throwable: Throwable, message: () -> String) = ifPlanted { Timber.i(throwable, message.invoke()) }

inline fun d(message: () -> String) = ifPlanted { Timber.d(message.invoke()) }
inline fun d(throwable: Throwable, message: () -> String) = ifPlanted { Timber.d(throwable, message.invoke()) }

inline fun v(message: () -> String) = ifPlanted { Timber.v(message.invoke()) }
inline fun v(throwable: Throwable, message: () -> String) = ifPlanted { Timber.v(throwable, message.invoke()) }

inline fun wtf(message: () -> String) = ifPlanted { Timber.wtf(message.invoke()) }
inline fun wtf(throwable: Throwable, message: () -> String) = ifPlanted { Timber.wtf(throwable, message.invoke()) }

inline fun log(priority: Int, t: Throwable, message: () -> String) {
    when (priority) {
        Log.ERROR -> e(t, message)
        Log.WARN -> w(t, message)
        Log.INFO -> i(t, message)
        Log.DEBUG -> d(t, message)
        Log.VERBOSE -> v(t, message)
        Log.ASSERT -> wtf(t, message)
    }
}

inline fun log(priority: Int, message: () -> String) {
    when (priority) {
        Log.ERROR -> e(message)
        Log.WARN -> w(message)
        Log.INFO -> i(message)
        Log.DEBUG -> d(message)
        Log.VERBOSE -> v(message)
        Log.ASSERT -> wtf(message)
    }
}