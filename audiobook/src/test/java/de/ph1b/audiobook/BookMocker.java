package de.ph1b.audiobook;

import java.io.File;
import java.util.List;
import java.util.Random;

import kotlin.collections.CollectionsKt;

/**
 * Mock provider for mocking objects and injecting them.
 *
 * @author Paul Woitaschek
 */
public class BookMocker {

   private static final Random rnd = new Random();

   public static Book mock(long id) {
      String root = "/root/";
      return mock(new File(root, "First.mp3"), new File(root, "/second.mp3"), id);
   }

   public static Book mock(File file1, File file2, long id) {
      Book.Type type = Book.Type.SINGLE_FOLDER;
      String author = "TestAuthor";
      int time = 0;
      String name = "TestBook";
      Chapter chapter1 = new Chapter(file1, file1.getName(), 1 + rnd.nextInt(100000));
      Chapter chapter2 = new Chapter(file2, file2.getName(), 1 + rnd.nextInt(200000));
      List<Chapter> chapters = CollectionsKt.listOf(chapter1, chapter2);
      float playbackSpeed = 1F;
      String root = file1.getParent();
      if (root == null) {
         root = "fakeRoot";
      }
      return new Book(id, type, author, file1, time, name, chapters, playbackSpeed, root);
   }
}
