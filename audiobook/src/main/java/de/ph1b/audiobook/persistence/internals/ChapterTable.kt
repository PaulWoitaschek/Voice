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

import android.database.sqlite.SQLiteDatabase

/**
 * Collection of strings representing the chapters table

 * @author Paul Woitaschek
 */
internal object ChapterTable {
    const val DURATION = "chapterDuration"
    const val NAME = "chapterName"
    const val PATH = "chapterPath"
    const val TABLE_NAME = "tableChapters"
    const val BOOK_ID = "bookId"
    private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ( " +
            "  $DURATION INTEGER NOT NULL, " +
            "  $NAME TEXT NOT NULL, " +
            "  $PATH TEXT NOT NULL, " +
            "  $BOOK_ID INTEGER NOT NULL, " +
            "  FOREIGN KEY ( $BOOK_ID ) REFERENCES ${BookTable.TABLE_NAME} ( ${BookTable.ID} )" +
            " )"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    fun dropTableIfExists(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
    }
}
