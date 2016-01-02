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

package de.ph1b.audiobook.view.fragment;

import android.test.ActivityInstrumentationTestCase2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.testing.DummyCreator;
import de.ph1b.audiobook.testing.TestApp;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Tests the BookShelfView.
 *
 * @author Paul Woitaschek
 */
public class BookShelfFragmentTest extends ActivityInstrumentationTestCase2<BookActivity> {

    private TestApp.BookShelfMockPresenter bookShelfMockPresenter;

    public BookShelfFragmentTest() {
        super(BookActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        bookShelfMockPresenter = ((TestApp) getActivity().getApplicationContext()).getBookShelfMockPresenter();
    }

    public void testBla() throws InterruptedException {
        final int COUNT = 1000;
        final CountDownLatch countDownLatch = new CountDownLatch(2 * COUNT);

        List<Book> books = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            books.add(DummyCreator.dummyBook(i));
        }
        bookShelfMockPresenter.newSet(books);

        Observable.range(1000, COUNT)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        bookShelfMockPresenter.addBook(DummyCreator.dummyBook(integer));
                        countDownLatch.countDown();
                    }
                });

        Observable.range(1, COUNT)
                .map(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer integer) {
                        return COUNT - integer;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        bookShelfMockPresenter.removed(DummyCreator.dummyBook(integer));
                        countDownLatch.countDown();
                    }
                });

        countDownLatch.await();
    }
}
