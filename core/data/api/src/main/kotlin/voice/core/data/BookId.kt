package voice.core.data

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BookIdSerializer::class)
public data class BookId(val value: String) {

  public constructor(uri: Uri) : this(uri.toString())

  public fun toUri(): Uri {
    return value.toUri()
  }
}

/** The scheme of books that live on an Audiobookshelf server instead of the device. */
public const val AUDIOBOOKSHELF_SCHEME: String = "abs"

/** Whether the book is streamed from a server rather than found on the device by the scanner. */
public val BookId.isRemote: Boolean
  get() = value.startsWith("$AUDIOBOOKSHELF_SCHEME:")

public fun BookId.isIn(folder: Uri): Boolean {
  val book = toUri()
  // a single file added on its own has a document uri, so nothing is in it
  return DocumentsContract.isTreeUri(folder) &&
    DocumentsContract.isTreeUri(book) &&
    book.authority == folder.authority &&
    DocumentsContract.getTreeDocumentId(book) == DocumentsContract.getTreeDocumentId(folder)
}

public object BookIdSerializer : KSerializer<BookId> {

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("bookId", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): BookId = BookId(decoder.decodeString())

  override fun serialize(
    encoder: Encoder,
    value: BookId,
  ) {
    encoder.encodeString(value.value)
  }
}
