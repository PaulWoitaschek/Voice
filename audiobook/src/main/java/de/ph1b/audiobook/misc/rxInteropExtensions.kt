package de.ph1b.audiobook.misc

import com.f2prateek.rx.preferences.Preference
import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.BackpressureStrategy

fun <T> rx.Observable<T>.toV2Observable(): io.reactivex.Observable<T> = RxJavaInterop.toV2Observable(this)
fun <T> io.reactivex.ObservableSource<T>.toV1Observable(): rx.Observable<T> = RxJavaInterop.toV1Observable(this, BackpressureStrategy.MISSING)
fun <T> Preference<T>.asV2Observable() = asObservable().toV2Observable()