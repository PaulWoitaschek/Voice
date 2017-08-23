package de.ph1b.audiobook.mvp

import io.reactivex.disposables.Disposable
import org.assertj.core.api.AbstractAssert

class DisposableAssert(actual: Disposable) : AbstractAssert<DisposableAssert, Disposable>(actual, DisposableAssert::class.java) {

  fun isDisposed(): DisposableAssert {
    if (!actual.isDisposed) {
      failWithMessage("Expected to be disposed, but it's not")
    }
    return this
  }

  fun isNotDisposed(): DisposableAssert {
    if (actual.isDisposed) {
      failWithMessage("Expected to be NOT disposed, but it is.")
    }
    return this
  }
}

fun assertThat(actual: Disposable) = DisposableAssert(actual)
