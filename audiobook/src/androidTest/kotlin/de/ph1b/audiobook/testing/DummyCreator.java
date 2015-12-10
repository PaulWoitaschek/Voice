package de.ph1b.audiobook.testing;

import android.os.Environment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;

/**
 * Mock provider for mocking objects and injecting them.
 *
 * @author Paul Woitaschek
 */
public class DummyCreator {

    private static final Random rnd = new Random();

    public static Book dummyBook(File file1, File file2) {
        long id = 1L;
        List<Bookmark> bookmarks = new ArrayList<>();
        Book.Type type = Book.Type.SINGLE_FILE;
        String author = "TestAuthor";
        int time = 0;
        String name = "TestBook";
        Chapter chapter1 = new Chapter(file1, file1.getName(), 1 + rnd.nextInt(100000));
        Chapter chapter2 = new Chapter(file2, file2.getName(), 1 + rnd.nextInt(200000));
        List<Chapter> chapters = Lists.newArrayList(chapter1, chapter2);
        float playbackSpeed = 1F;
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        return new Book(id,
                ImmutableList.copyOf(bookmarks),
                type,
                false,
                author,
                file1,
                time,
                name,
                ImmutableList.copyOf(chapters),
                playbackSpeed,
                root);
    }
}
