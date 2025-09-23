package voice.core.data.repo

import voice.core.data.Book

public interface BookSearchRepo {
  public suspend fun search(query: String): List<Book>
}
