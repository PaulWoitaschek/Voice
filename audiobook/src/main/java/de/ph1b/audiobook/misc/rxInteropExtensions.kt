package de.ph1b.audiobook.misc

import com.f2prateek.rx.preferences.Preference
import hu.akarnokd.rxjava.interop.RxJavaInterop

// convert from 1.x to 2.x
fun <T> rx.Observable<T>.toV2Flowable(): io.reactivex.Flowable<T> = RxJavaInterop.toV2Flowable(this)

fun <T> rx.Observable<T>.toV2Observable(): io.reactivex.Observable<T> = RxJavaInterop.toV2Observable(this)
fun <T> rx.Single<T>.toV2Single(): io.reactivex.Single<T> = RxJavaInterop.toV2Single(this)
fun <T> rx.Completable.toV2Completable(): io.reactivex.Completable = RxJavaInterop.toV2Completable(this)
fun <T> rx.Single<T>.toV2Maybe(): io.reactivex.Maybe<T> = RxJavaInterop.toV2Maybe(this)
fun <T> rx.Completable.toV2Maybe(): io.reactivex.Maybe<T> = RxJavaInterop.toV2Maybe(this)

// convert from 2.x to 1.x
fun <T> org.reactivestreams.Publisher<T>.toV1Observable(): rx.Observable<T> = RxJavaInterop.toV1Observable(this)

fun <T> io.reactivex.ObservableSource<T>.toV1Observable(strategy: io.reactivex.BackpressureStrategy): rx.Observable<T> =
        RxJavaInterop.toV1Observable(this, strategy)

fun <T> io.reactivex.SingleSource<T>.toV1Single(): rx.Single<T> = RxJavaInterop.toV1Single(this)
fun <T> io.reactivex.CompletableSource.toV1Completable(): rx.Completable = RxJavaInterop.toV1Completable(this)
fun <T> io.reactivex.MaybeSource<T>.toV1Single(): rx.Single<T> = RxJavaInterop.toV1Single(this)
fun <T> io.reactivex.MaybeSource<T>.toV1Completable(): rx.Completable = RxJavaInterop.toV1Completable(this)

fun <T> Preference<T>.asV2Observable() = asObservable().toV2Observable()