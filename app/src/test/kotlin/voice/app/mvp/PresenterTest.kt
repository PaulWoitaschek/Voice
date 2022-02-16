package voice.app.mvp

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class PresenterTest {

  private lateinit var presenter: Presenter<View>

  @Before
  fun setUp() {
    presenter = object : Presenter<View>() {}
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
}
