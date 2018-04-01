package de.ph1b.audiobook

import de.ph1b.audiobook.persistence.pref.Pref
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.KProperty

class MemoryPref<T : Any>(default: T) : Pref<T>() {

  private val subject = BehaviorSubject.createDefault(default)

  override val stream: Observable<T> = subject.hide()!!

  override fun getValue(thisRef: Any, property: KProperty<*>): T = subject.value!!

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    subject.onNext(value)
  }
}
