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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.testing;

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
        Book.Type type = Book.Type.SINGLE_FOLDER;
        String author = "TestAuthor";
        int time = 0;
        String name = "TestBook";
        Chapter chapter1 = new Chapter(file1, file1.getName(), 1 + rnd.nextInt(100000));
        Chapter chapter2 = new Chapter(file2, file2.getName(), 1 + rnd.nextInt(200000));
        List<Chapter> chapters = Lists.newArrayList(chapter1, chapter2);
        float playbackSpeed = 1F;
        String root = file1.getParent();
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
