package de.ph1b.audiobook.utils;


import android.app.Activity;

import java.util.ArrayList;

import de.ph1b.audiobook.content.Book;

public class BookUtil {

    private final BaseApplication baseApplication;

    public BookUtil(Activity a) {
        baseApplication = ((BaseApplication) a.getApplication());
    }

    public ArrayList<Book> getAllBooks() {
        return baseApplication.getAllBooks();
    }

    public Book getCurrentBook() {
        return baseApplication.getCurrentBook();
    }

    public void setCurrentBookId(long bookId) {
        baseApplication.setCurrentBook(bookId);
    }

    public void addOnPlayStateChangedListener(BaseApplication.OnPlayStateChangedListener listener) {
        baseApplication.addOnPlayStateChangedListener(listener);
    }

    public void removeOnPlayStateChangedListener(BaseApplication.OnPlayStateChangedListener listener) {
        baseApplication.removeOnPlayStateChangedListener(listener);
    }
}
