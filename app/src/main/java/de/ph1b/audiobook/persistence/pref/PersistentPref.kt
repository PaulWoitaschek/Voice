package de.ph1b.audiobook.persistence.pref

import com.f2prateek.rx.preferences2.Preference
import de.ph1b.audiobook.common.latestAsFlow
import de.ph1b.audiobook.prefs.Pref
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty

class PersistentPref<T : Any>(private val pref: Preference<T>) : Pref<T>() {

  override val flow: Flow<T> get() = pref.asObservable().latestAsFlow()

  override fun getValue(thisRef: Any, property: KProperty<*>): T = pref.get()

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    pref.set(value)
  }
}
