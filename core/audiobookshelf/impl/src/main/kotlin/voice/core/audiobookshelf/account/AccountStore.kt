package voice.core.audiobookshelf.account

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import voice.core.audiobookshelf.audiobookshelfJson
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Serializable
internal data class Account(
  val serverUrl: String,
  val userId: String,
  val username: String,
  val accessToken: String,
  val refreshToken: String,
  val libraryIds: List<String>,
  val serverVersion: String,
  /** The server no longer accepts the tokens, so the listener has to sign in again. */
  val needsLogin: Boolean = false,
)

@Serializable
internal data class Preferences(
  val downloadOverMobileData: Boolean = false,
  /** Tells the server which listening sessions came from this device. */
  val deviceId: String? = null,
  /** Voice asks for notifications once, at the first download. */
  val askedForNotifications: Boolean = false,
)

/**
 * What Voice last agreed on with the server for a book. A local position that moved away from it was played on
 * this device, a newer [serverLastUpdate] means another device listened.
 */
@Serializable
internal data class SyncedProgress(
  val positionMs: Long,
  val finished: Boolean,
  val serverLastUpdate: Long,
)

@SingleIn(AppScope::class)
internal class AccountStore(store: DataStore<Account?>) : DataStore<Account?> by store {

  @Inject
  constructor(context: Context) : this(
    jsonStore(
      context = context,
      fileName = "account",
      serializer = Account.serializer().nullable,
      defaultValue = null,
    ),
  )
}

@SingleIn(AppScope::class)
internal class PreferencesStore(store: DataStore<Preferences>) : DataStore<Preferences> by store {

  @Inject
  constructor(context: Context) : this(
    jsonStore(
      context = context,
      fileName = "preferences",
      serializer = Preferences.serializer(),
      defaultValue = Preferences(),
    ),
  )
}

@SingleIn(AppScope::class)
internal class SyncedProgressStore(store: DataStore<Map<String, SyncedProgress>>) : DataStore<Map<String, SyncedProgress>> by store {

  @Inject
  constructor(context: Context) : this(
    jsonStore(
      context = context,
      fileName = "syncedProgress",
      serializer = MapSerializer(String.serializer(), SyncedProgress.serializer()),
      defaultValue = emptyMap(),
    ),
  )
}

/**
 * The bookmarks both sides had at the last sync, with their titles. A bookmark that is missing on one side was
 * deleted there when it is in here, and is new on the other side when it isn't.
 */
@SingleIn(AppScope::class)
internal class SyncedBookmarksStore(store: DataStore<Map<String, String>>) : DataStore<Map<String, String>> by store {

  @Inject
  constructor(context: Context) : this(
    jsonStore(
      context = context,
      fileName = "syncedBookmarks",
      serializer = MapSerializer(String.serializer(), String.serializer()),
      defaultValue = emptyMap(),
    ),
  )
}

private fun <T> jsonStore(
  context: Context,
  fileName: String,
  serializer: KSerializer<T>,
  defaultValue: T,
): DataStore<T> {
  return DataStoreFactory.create(
    serializer = object : Serializer<T> {
      override val defaultValue: T = defaultValue

      override suspend fun readFrom(input: InputStream): T {
        return try {
          audiobookshelfJson.decodeFromString(serializer, input.readBytes().decodeToString())
        } catch (e: SerializationException) {
          throw CorruptionException("Could not read $fileName", e)
        }
      }

      override suspend fun writeTo(
        t: T,
        output: OutputStream,
      ) {
        output.write(audiobookshelfJson.encodeToString(serializer, t).encodeToByteArray())
      }
    },
  ) {
    File(context.filesDir, "audiobookshelf/$fileName.json")
  }
}
