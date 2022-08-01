package voice.app.serialization

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
class KotlinxDataStoreSerializer<T>(
  override val defaultValue: T,
  private val json: Json,
  private val serializer: KSerializer<T>,
) : Serializer<T> {

  override suspend fun readFrom(input: InputStream): T = json.decodeFromStream(serializer, input)

  override suspend fun writeTo(t: T, output: OutputStream) {
    json.encodeToStream(serializer, t, output)
  }
}
