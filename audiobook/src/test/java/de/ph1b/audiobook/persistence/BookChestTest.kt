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

package de.ph1b.audiobook.persistence

import android.os.Build
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.DummyCreator
import de.ph1b.audiobook.persistence.internals.InternalBookRegister
import de.ph1b.audiobook.persistence.internals.InternalDb
import junit.framework.TestCase
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TODO:
 *
 * @author: Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml")
class BookChestTest : TestCase() {

    @Test
    fun testBookChest() {
        val dbName = System.currentTimeMillis().toString()
        val internalDb = InternalDb(RuntimeEnvironment.application, dbName)
        val internalBookRegister = InternalBookRegister(internalDb)
        val bookChest = BookChest(internalBookRegister)

        var dummy = DummyCreator.dummyBook(5)
        bookChest.addBook(dummy)
        val firstBook = bookChest.activeBooks.first()
        var dummyWithUpdatedId = dummy.copy(id = firstBook.id)

        assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
    }
}