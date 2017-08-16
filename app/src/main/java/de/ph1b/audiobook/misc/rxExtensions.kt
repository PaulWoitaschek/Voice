package de.ph1b.audiobook.misc

import com.f2prateek.rx.preferences.Preference
import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun <T> rx.Observable<T>.toV2Observable(): io.reactivex.Observable<T> = RxJavaInterop.toV2Observable(this)
fun <T> Preference<T>.asV2Observable() = asObservable().toV2Observable()
fun Disposable.addTo(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)
