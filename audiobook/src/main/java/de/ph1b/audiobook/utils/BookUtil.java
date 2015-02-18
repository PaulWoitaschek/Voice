package de.ph1b.audiobook.utils;


import android.app.Activity;

import java.util.ArrayList;

import de.ph1b.audiobook.content.Book;

public class BookUtil {

    public static ArrayList<Book> getAllBooks(Activity a) {
        return ((BaseApplication) a.getApplication()).getAllBooks();
    }
}
