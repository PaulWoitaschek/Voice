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

package de.ph1b.audiobook.testing

/**
 * Created by ph1b on 17.12.15.
 */
class RealFileMocker {

    lateinit var file1: File
    lateinit var file2: File

    public fun create(context: Context): List<File> {
        val externalStorage = Environment.getExternalStorageDirectory()

        val parentFolder = File(externalStorage, "testFolder")
        parentFolder.mkdirs()
        file1 = File(parentFolder, "1.mp3")
        file2 = File(parentFolder, "2.mp3")

        ByteStreams.copy(context.assets.open("3rdState.mp3"), FileOutputStream(file1))
        ByteStreams.copy(context.assets.open("Crashed.mp3"), FileOutputStream(file2))

        return listOf(file1, file2);
    }

    public fun destroy() {
        file1.delete();
        file2.delete();
    }
}