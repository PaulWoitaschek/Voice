package de.ph1b.audiobook.mvp

import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

//import org.assertj.core.api.Assertions.assertThat


class PresenterTest {

  private lateinit var presenter: Presenter<View>

  @Before
  fun setUp() {
    presenter = object : Presenter<View>() {}
  }

  @Test
  fun addWhileAttachedDoesNotDispose() {
    presenter.attach(View)
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    assertThatDisposable(disposable).isNotDisposed()
  }

  @Test
  fun addAfterDetachDisposes() {
    presenter.attach(View)
    presenter.detach()
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    assertThatDisposable(disposable).isDisposed()
  }

  @Test
  fun addBeforeAttachDisposes() {
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    assertThatDisposable(disposable).isDisposed()
  }

  @Test
  fun replaceBeforeAttachDisposes() {
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    assertThatDisposable(disposable).isDisposed()
  }

  @Test
  fun detachDisposesAdded() {
    presenter.attach(View)
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    presenter.detach()
    assertThatDisposable(disposable).isDisposed()
  }

  @Test
  fun attached() {
    assertThat(presenter.attached).isFalse()
    presenter.attach(View)
    assertThat(presenter.attached).isTrue()
    presenter.detach()
    assertThat(presenter.attached).isFalse()
  }

  private object View

  private fun Presenter<*>.disposeOnDetach(disposable: Disposable) {
    with(this) {
      disposable.disposeOnDetach()
    }
  }
}
