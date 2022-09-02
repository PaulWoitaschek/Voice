package voice.app.mvp

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
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
    presenter.attached.shouldBeFalse()
    presenter.attach(View)
    presenter.attached.shouldBeTrue()
    presenter.detach()
    presenter.attached.shouldBeFalse()
  }

  private object View
}
