package de.ph1b.audiobook.testing;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.injection.AndroidModule;
import de.ph1b.audiobook.injection.BaseModule;
import de.ph1b.audiobook.mediaplayer.MediaPlayerControllerTest;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.persistence.BookShelfTest;

/**
 * Mock provider for mocking objects and injecting them.
 *
 * @author Paul Woitaschek
 */
public class MockProvider {

    private final Random rnd = new Random();
    private final Context context;

    public MockProvider(Context context) {
        this.context = context;
    }

    public MockComponent newMockComponent() {
        return DaggerMockProvider_MockComponent.builder()
                .baseModule(new BaseModule())
                .androidModule(new AndroidModule((Application) context.getApplicationContext()))
                .build();
    }

    public Book dummyBook(File file1, File file2) {
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

    @Singleton
    @Component(modules = {BaseModule.class, AndroidModule.class})
    public interface MockComponent {

        void inject(MediaPlayerControllerTest target);

        void inject(BookShelfTest target);
    }
}
