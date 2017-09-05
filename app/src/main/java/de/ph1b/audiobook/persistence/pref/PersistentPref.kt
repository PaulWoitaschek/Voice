package de.ph1b.audiobook.persistence.pref

import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.misc.toV2Observable
import io.reactivex.Observable
import kotlin.reflect.KProperty

class PersistentPref<T : Any>(private val pref: Preference<T>) : Pref<T>() {

  override val stream: Observable<T> = pref.asObservable().toV2Observable()

  override fun getValue(thisRef: Any, property: KProperty<*>): T = pref.get()!!

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    pref.set(value)
  }
}
