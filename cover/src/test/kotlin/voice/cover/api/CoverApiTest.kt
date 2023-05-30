package voice.cover.api

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CoverApiTest {

  @Test
  fun test() = runTest {
    val api = CoverApi(
      CoverModule.internalApi(CoverModule.client()),
    )
    val query = "unicorns"
    val token = api.token(query).shouldNotBeEmpty()!!
    api.search(query = query, auth = token)
      .results
      .shouldNotBeEmpty()
  }
}
