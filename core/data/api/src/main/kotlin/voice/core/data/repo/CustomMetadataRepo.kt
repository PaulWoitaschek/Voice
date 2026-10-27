package voice.core.data.repo

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.CustomMetadata

public interface CustomMetadataRepo {

  public fun flow(): Flow<Map<BookId, CustomMetadata>>

  public fun flow(id: BookId): Flow<CustomMetadata>

  public suspend fun get(id: BookId): CustomMetadata

  public suspend fun put(custom: CustomMetadata)
}
