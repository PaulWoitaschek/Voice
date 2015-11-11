package de.ph1b.audiobook.utils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.BookShelf;

/**
 * Simple helper that provides blocking access to {@link BookShelf}
 *
 * @author Paul Woitaschek
 */
@Singleton
public class BookVendor {

    private final BookShelf bookShelf;

    @Inject
    public BookVendor(BookShelf bookShelf) {
        this.bookShelf = bookShelf;
    }

    public Book byId(long id) {
        return bookShelf.getActiveBooks()
                .singleOrDefault(null, book -> book.id() == id)
                .toBlocking()
                .single();
    }

    public List<Book> all() {
        return bookShelf.getActiveBooks()
                .toList()
                .toBlocking()
                .first();
    }
}
