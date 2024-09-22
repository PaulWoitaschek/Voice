package voice.review

import android.content.Context
import javax.inject.Inject

class ReviewTranslated
@Inject constructor(private val context: Context) {

  fun translated(): Boolean {
    val language = context.resources.configuration.locales[0].language.lowercase()
    return language in listOf("en", "de", "es", "ru", "it", "ar")
  }
}
