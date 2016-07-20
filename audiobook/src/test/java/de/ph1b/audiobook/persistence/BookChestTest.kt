package de.ph1b.audiobook.persistence

import android.os.Build
import de.ph1b.audiobook.BookMocker
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.TestApp
import de.ph1b.audiobook.persistence.internals.InternalBookRegister
import de.ph1b.audiobook.persistence.internals.InternalDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Test for the book chest.
 *
 * @author: Paul Woitaschek
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class BookChestTest {

    init {
        ShadowLog.stream = System.out
    }

    private lateinit var bookChest: BookChest

    @Before
    fun setUp() {
        val internalDb = InternalDb(RuntimeEnvironment.application)
        val internalBookRegister = InternalBookRegister(internalDb)
        bookChest = BookChest(internalBookRegister)
    }

    @Test
    fun testBookChest() {
        val dummy = BookMocker.mock(5)
        bookChest.addBook(dummy)
        val firstBook = bookChest.activeBooks.first()
        val dummyWithUpdatedId = dummy.copy(id = firstBook.id)

        assertThat(dummyWithUpdatedId).isEqualTo(firstBook)
    }
}