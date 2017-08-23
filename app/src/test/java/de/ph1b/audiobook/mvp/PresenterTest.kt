package de.ph1b.audiobook.mvp

import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.junit.Before
import org.junit.Test

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
    assertThat(disposable).isNotDisposed()
  }

  @Test(expected = IllegalStateException::class)
  fun addAfterDetachThrows() {
    presenter.attach(View)
    presenter.detach()
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
  }

  @Test(expected = IllegalStateException::class)
  fun addBeforeAttachThrows() {
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
  }

  @Test(expected = IllegalStateException::class)
  fun replaceBeforeAttachThrows() {
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
  }

  @Test
  fun detachDisposesAdded() {
    presenter.attach(View)
    val disposable = Disposables.empty()
    presenter.disposeOnDetach(disposable)
    presenter.detach()
    assertThat(disposable).isDisposed()
  }

  private object View

  private fun Presenter<*>.disposeOnDetach(disposable: Disposable) {
    with(this) {
      disposable.disposeOnDetach()
    }
  }
}
