package de.ph1b.audiobook.mvp

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import de.ph1b.audiobook.mvp.DisposableSubject.Companion.factory
import io.reactivex.disposables.Disposable

class DisposableSubject(failureMetadata: FailureMetadata, private val actual: Disposable?) :
  Subject(failureMetadata, actual) {

  fun isDisposed(): DisposableSubject {
    if (actual?.isDisposed != true) {
      failWithActual("expected to be disposed", true)
    }
    return this
  }

  fun isNotDisposed(): DisposableSubject {
    if (actual?.isDisposed != false) {
      failWithActual("expected to be disposed", false)
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
