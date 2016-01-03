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

package de.ph1b.audiobook.view.fragment

import android.test.ActivityInstrumentationTestCase2
import android.test.suitebuilder.annotation.MediumTest
import com.robotium.solo.Solo
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.testing.DummyCreator
import de.ph1b.audiobook.testing.TestApp
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests the BookShelfView.

 * @author Paul Woitaschek
 */
class BookShelfFragmentTest : ActivityInstrumentationTestCase2<BookActivity>(BookActivity::class.java) {

    private lateinit var bookShelfMockPresenter: TestApp.BookShelfMockPresenter
    private lateinit var solo: Solo
    private var random = Random()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        solo = Solo(instrumentation, activity)

        bookShelfMockPresenter = (activity.applicationContext as TestApp).bookShelfMockPresenter
    }

    private fun randomBooks(amount: Int): List<Book> {
        val books = ArrayList<Book>()
        for (i in 0..amount - 1) {
            books.add(DummyCreator.dummyBook(i.toLong()))
        }
        return books
    }

    @MediumTest
    @Throws(InterruptedException::class)
    fun testRestart() {
        solo.waitForActivity(BookActivity::class.java)

        val AMOUNT_OF_BOOKS = 50

        bookShelfMockPresenter
                .onBindSubject
                .map { random.nextInt(AMOUNT_OF_BOOKS) }
                .map { randomBooks(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { bookShelfMockPresenter.newSet(it) }

        bookShelfMockPresenter
                .onBindSubject
                .flatMap { Observable.range(0, AMOUNT_OF_BOOKS) }
                .map { AMOUNT_OF_BOOKS - it }
                .map { DummyCreator.dummyBook(it.toLong()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { bookShelfMockPresenter.addBook(it) }

        bookShelfMockPresenter
                .onBindSubject
                .flatMap { Observable.range(0, AMOUNT_OF_BOOKS) }
                .map { DummyCreator.dummyBook(it.toLong()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { bookShelfMockPresenter.removed(it) }

        val times = 100
        val latch = CountDownLatch(times)
        var lastBoolean = false
        Observable.interval(200, TimeUnit.MILLISECONDS)
                .take(times)
                .doOnNext { lastBoolean = lastBoolean.not() }
                .map { if (lastBoolean) Solo.LANDSCAPE else Solo.PORTRAIT } // landscape if even
                .subscribe {
                    solo.setActivityOrientation(it)
                    if (latch.count == 1L) {
                        Thread.sleep(2000)
                    }
                    latch.countDown()
                }
        latch.await()
    }

    @MediumTest
    @Throws(InterruptedException::class)
    fun testAddingRemoving() {
        val COUNT = 1000
        val countDownLatch = CountDownLatch(2 * COUNT)

        val newBooks = ArrayList<Book>()
        for (i in 0..COUNT - 1) {
            newBooks.add(DummyCreator.dummyBook(i.toLong()))
        }
        bookShelfMockPresenter.newSet(newBooks)

        Observable.range(1000, COUNT)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { integer ->
                    bookShelfMockPresenter.addBook(DummyCreator.dummyBook(integer!!.toLong()))
                    countDownLatch.countDown()
                }

        Observable.range(1, COUNT)
                .map { integer -> COUNT - integer!! }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { integer ->
                    bookShelfMockPresenter.removed(DummyCreator.dummyBook(integer!!.toLong()))
                    countDownLatch.countDown()
                }

        countDownLatch.await()
    }

    @Throws(Exception::class)
    override fun tearDown() {
        solo.finishOpenedActivities()

        super.tearDown()
    }
}
