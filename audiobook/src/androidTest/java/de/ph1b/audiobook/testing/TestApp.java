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

package de.ph1b.audiobook.testing;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import de.ph1b.audiobook.injection.AndroidModule;
import de.ph1b.audiobook.injection.App;
import de.ph1b.audiobook.injection.BaseModule;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.presenter.BookShelfBasePresenter;
import de.ph1b.audiobook.view.fragment.BookShelfFragment;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Subclass of the Application that provides an entry point for overriding provided dependencies..
 *
 * @author Paul Woitaschek
 */
public class TestApp extends App {

    private final BookShelfMockPresenter bookShelfPresenter = new BookShelfMockPresenter();

    public BookShelfMockPresenter getBookShelfMockPresenter() {
        return bookShelfPresenter;
    }

    @Override
    protected ApplicationComponent newComponent() {
        return DaggerTestApp_MockComponent.builder()
                .androidModule(new AndroidModule(this))
                .mockPresenterModule(new MockPresenterModule(bookShelfPresenter))
                .build();
    }

    @Singleton
    @Component(modules = {BaseModule.class, AndroidModule.class, MockPresenterModule.class})
    public interface MockComponent extends App.ApplicationComponent {

    }

    @Module
    public static class MockPresenterModule {

        private final BookShelfBasePresenter bookShelfBasePresenter;

        public MockPresenterModule(BookShelfBasePresenter bookShelfBasePresenter) {
            this.bookShelfBasePresenter = bookShelfBasePresenter;
        }

        @Provides
        BookShelfBasePresenter provideBookShelfBasePresenter() {
            return bookShelfBasePresenter;
        }
    }

    public static class BookShelfMockPresenter extends BookShelfBasePresenter {

        public PublishSubject<Void> onBindSubject = PublishSubject.create();

        public void addBook(Book book) {
            if (getView() != null) getView().bookAddedOrUpdated(book);
        }

        public void newSet(List<Book> newBooks) {
            if (getView() != null) getView().newBooks(newBooks);
        }

        public void showSpinnerIfNoData(boolean visible) {
            if (getView() != null) getView().showSpinnerIfNoData(visible);
        }

        public void onCurrentBookChanged(Book book) {
            if (getView() != null) getView().currentBookChanged(book);
        }

        public void removed(Book book) {
            if (getView() != null) getView().bookRemoved(book);
        }

        @Override
        public void playPauseRequested() {

        }

        @Override
        public void onBind(BookShelfFragment view, @NotNull CompositeSubscription subscriptions) {
            onBindSubject.onNext(null);
        }
    }
}
