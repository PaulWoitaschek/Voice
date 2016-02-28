/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.persistence.internals

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import de.ph1b.audiobook.model.Bookmark
import java.io.File

fun Bookmark.toContentValues() = ContentValues().apply {
    put(BookmarkTable.TIME, time)
    put(BookmarkTable.PATH, mediaFile.absolutePath)
    put(BookmarkTable.TITLE, title)
    if (id != Bookmark.ID_UNKNOWN) put(BookmarkTable.ID, id)
}

fun Cursor.toBookmark(): Bookmark {
    val path = string(BookmarkTable.PATH)
    val title = string(BookmarkTable.TITLE)
    val time = int(BookmarkTable.TIME)
    val id = long(BookmarkTable.ID)
    return Bookmark(File(path), title, time, id)
}

/**
 * Represents an sql table for bookmarks
 *
 * @author Paul Woitaschek
 */
object BookmarkTable {

    const val PATH = "bookmarkPath"
    const val TITLE = "bookmarkTitle"
    const val TABLE_NAME = "tableBookmarks"
    const val TIME = "bookmarkTime"
    const val ID = BaseColumns._ID
    private val CREATE_TABLE = "CREATE TABLE ${TABLE_NAME} ( " +
            " ${ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " ${PATH} TEXT NOT NULL, " +
            " ${TITLE} TEXT NOT NULL, " +
            " ${TIME} INTEGER NOT NULL)"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS ${TABLE_NAME}")
    }
}