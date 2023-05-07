package voice.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

class VoiceDataStoreFactory
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

  fun int(
    fileName: String,
    defaultValue: Int,
  ): DataStore<Int> {
    return create(
      serializer = Int.serializer(),
      defaultValue = defaultValue,
      fileName = fileName,
    )
  }
}
