package voice.app.serialization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SerializableDataStoreFactory
@Inject constructor(
  private val json: Json,
  private val context: Context,
) {

  fun <T> create(
    serializer: KSerializer<T>,
    defaultValue: T,
    fileName: String,
  ): DataStore<T> {
    return DataStoreFactory.create(
      serializer = KotlinxDataStoreSerializer(
        defaultValue = defaultValue,
        json = json,
        serializer = serializer,
      ),
    ) {
      context.dataStoreFile(fileName)
    }
  }
}
