package voice.core.data.repo.internals.migrations

import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.mapRows

/**
 * Takes the file names of the chapters from their documents where these hold the path. A book moved before the next
 * scan can then still be found by its files, as its old files aren't there to be scanned anymore.
 */
internal class Migration66 : AutoMigrationSpec {

  override fun onPostMigrate(db: SupportSQLiteDatabase) {
    val update = db.compileStatement("UPDATE chapters2 SET fileName = ? WHERE id = ?")
    db.query("SELECT id FROM chapters2 WHERE id LIKE 'file:%' OR id LIKE 'content://$EXTERNAL_STORAGE/%'")
      .mapRows { getString("id") }
      .forEach { id ->
        val fileName = fileNameInPath(id) ?: return@forEach
        update.bindString(1, fileName)
        update.bindString(2, id)
        update.executeUpdateDelete()
      }
  }
}

private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"

private fun fileNameInPath(chapterId: String): String? {
  val uri = chapterId.toUri()
  return when {
    uri.scheme == "file" -> uri.lastPathSegment
    uri.authority == EXTERNAL_STORAGE -> try {
      DocumentsContract.getDocumentId(uri).substringAfter(':').substringAfterLast('/')
    } catch (_: IllegalArgumentException) {
      null
    }
    else -> null
  }?.takeIf { it.isNotEmpty() }
}
