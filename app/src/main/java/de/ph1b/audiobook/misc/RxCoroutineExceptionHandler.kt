package de.ph1b.audiobook.misc

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxSingle
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private object RxCoroutineExceptionHandler :
  AbstractCoroutineContextElement(CoroutineExceptionHandler),
  CoroutineExceptionHandler {

  override fun handleException(context: CoroutineContext, exception: Throwable) {
    if (exception is CancellationException) return
    RxJavaPlugins.onError(exception)
  }
}

object RxScope : CoroutineScope {

  override val coroutineContext: CoroutineContext = RxCoroutineExceptionHandler
}

fun <T : Any> rxSingle(
  context: CoroutineContext = EmptyCoroutineContext,
  block: suspend CoroutineScope.() -> T
): Single<T> {
  return RxScope.rxSingle(context, block)
}

fun rxCompletable(
  context: CoroutineContext = EmptyCoroutineContext,
  block: suspend CoroutineScope.() -> Unit
): Completable {
  return RxScope.rxCompletable(context, block)
}
