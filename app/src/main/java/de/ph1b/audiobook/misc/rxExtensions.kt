package de.ph1b.audiobook.misc

import com.f2prateek.rx.preferences.Preference
import hu.akarnokd.rxjava.interop.RxJavaInterop

fun <T> rx.Observable<T>.toV2Observable(): io.reactivex.Observable<T> = RxJavaInterop.toV2Observable(this)
fun <T> Preference<T>.asV2Observable() = asObservable().toV2Observable()
