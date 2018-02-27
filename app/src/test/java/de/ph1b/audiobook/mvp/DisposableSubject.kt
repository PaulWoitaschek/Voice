package de.ph1b.audiobook.mvp

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import de.ph1b.audiobook.mvp.DisposableSubject.Companion.factory
import io.reactivex.disposables.Disposable

class DisposableSubject(failureMetadata: FailureMetadata, actual: Disposable?) :
  Subject<DisposableSubject, Disposable>(failureMetadata, actual) {

  fun isDisposed(): DisposableSubject {
    if (actual()?.isDisposed != true) {
      fail("Expected to be disposed, but it's not")
    }
    return this
  }

  fun isNotDisposed(): DisposableSubject {
    if (actual()?.isDisposed != false) {
      fail("Expected to be NOT disposed, but it is.")
    }
    return this
  }

  companion object {
    val factory = Factory<DisposableSubject, Disposable> { metadata, actual ->
      DisposableSubject(
        metadata,
        actual
      )
    }
  }
}

fun Disposable?.assert(): DisposableSubject = assertAbout(factory).that(this)
