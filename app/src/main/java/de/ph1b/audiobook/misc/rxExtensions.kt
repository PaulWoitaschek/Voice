package de.ph1b.audiobook.misc

import hu.akarnokd.rxjava.interop.RxJavaInterop
import rx.Observable as V1Observable

fun <T> V1Observable<T>.toV2Observable(): io.reactivex.Observable<T> =
  RxJavaInterop.toV2Observable(this)
