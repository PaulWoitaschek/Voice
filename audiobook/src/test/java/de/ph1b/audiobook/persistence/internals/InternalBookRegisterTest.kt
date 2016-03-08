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

import android.os.Build
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.DummyCreator
import de.ph1b.audiobook.TestApp
import org.fest.assertions.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog


/**
 * Simple test for book persistence.
 *
 * @author Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class InternalBookRegisterTest {

    init {
        ShadowLog.stream = System.out
    }

    lateinit var register: InternalBookRegister

    @Before
    fun setUp() {
        val internalDb = InternalDb(RuntimeEnvironment.application, "db")
        register = InternalBookRegister(internalDb)
    }

    @Test
    fun testHideRevealBook() {
        val dummy = DummyCreator.dummyBook(-1)
        register.addBook(dummy)

        val activeBooks = register.activeBooks()
        val inactiveBooks = register.orphanedBooks()

        assertThat(activeBooks).hasSize(1)
        assertThat(inactiveBooks).isEmpty()

        register.hideBook(activeBooks.first().id)

        assertThat(register.activeBooks()).isEmpty()
        assertThat(register.orphanedBooks()).hasSize(1)

        val hiddenBook = register.orphanedBooks().single()

        register.revealBook(hiddenBook.id)

        assertThat(register.activeBooks()).hasSize(1)
        assertThat(register.orphanedBooks()).isEmpty()
    }
}