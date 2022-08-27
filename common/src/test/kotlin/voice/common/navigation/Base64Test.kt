package voice.common.navigation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class Base64Test {

  @Test
  fun conversion() {
    val cats = "cats"
    cats.base64Encoded() shouldNotBe cats
    cats.base64Encoded().base64Decoded() shouldBe cats
  }
}
