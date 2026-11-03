package voice.core.data

import java.util.concurrent.TimeUnit.SECONDS

public enum class BookCategory {
  CURRENT,
  NOT_STARTED,
  FINISHED,
}

public val Book.category: BookCategory
  get() {
    return if (position == 0L) {
      BookCategory.NOT_STARTED
    } else {
      if (position >= duration - SECONDS.toMillis(5)) {
        BookCategory.FINISHED
      } else {
        BookCategory.CURRENT
      }
    }
  }
