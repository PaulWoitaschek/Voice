package de.ph1b.audiobook.misc

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4

object Observables {

  inline fun <T1 : Any, T2 : Any, R : Any> combineLatest(
      source1: ObservableSource<T1>,
      source2: ObservableSource<T2>,
      crossinline combiner: (T1, T2) -> R
  ): Observable<R> {
    return Observable.combineLatest(source1, source2, BiFunction { t1, t2 -> combiner(t1, t2) })
  }

  inline fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, R : Any> combineLatest(
      source1: ObservableSource<T1>,
      source2: ObservableSource<T2>,
      source3: ObservableSource<T3>,
      source4: ObservableSource<T4>,
      crossinline combiner: (T1, T2, T3, T4) -> R
  ): Observable<R> {
    return Observable.combineLatest(source1, source2, source3, source4, Function4 { t1, t2, t3, t4 -> combiner(t1, t2, t3, t4) })
  }
}
