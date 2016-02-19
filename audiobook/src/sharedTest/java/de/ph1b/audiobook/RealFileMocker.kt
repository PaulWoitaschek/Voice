package de.ph1b.audiobook
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


import android.content.Context
import android.os.Environment
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


class RealFileMocker {

    lateinit var file1: File
    lateinit var file2: File

    fun create(context: Context): List<File> {
        val storage = context.filesDir

        val parentFolder = File(storage, "testFolder")
        parentFolder.mkdirs()
        file1 = File(parentFolder, "1.mp3")
        file2 = File(parentFolder, "2.mp3")

        val request = Request.Builder().url("http://mirrors.creativecommons.org/ccmixter/contrib/Wired/Beastie%20Boys%20-%20Now%20Get%20Busy.mp3")
                .build();
        val response = OkHttpClient().newCall(request).execute();

        val inStream = response.body().byteStream()
        inStream.copyTo(FileOutputStream(file1))
        file1.copyTo(file2)

        check(file1.exists())
        check(file2.exists())

        return listOf(file1, file2);
    }

    fun destroy() {
        file1.delete();
        file2.delete();
    }
}