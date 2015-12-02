package de.ph1b.audiobook.testing

import android.app.Application
import android.content.Context
import android.os.Environment
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import dagger.Component
import de.ph1b.audiobook.injection.AndroidModule
import de.ph1b.audiobook.injection.BaseModule
import de.ph1b.audiobook.mediaplayer.MediaPlayerControllerTest
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.Bookmark
import de.ph1b.audiobook.model.Chapter
import de.ph1b.audiobook.persistence.BookShelfTest
import java.io.File
import java.util.*
import javax.inject.Singleton

/**
 * Providing mocked components with mocked modules
 *
 * @author Paul Woitaschek
 */
class MockProvider(val context: Context) {

    fun newMockComponent(): MockComponent {
        return DaggerMockProvider_MockComponent.builder()
                .androidModule(AndroidModule(context.applicationContext as Application))
                .baseModule(BaseModule())
                .build()
    }

    @Singleton
    @Component(modules = arrayOf(BaseModule::class, AndroidModule::class))
    interface MockComponent {

        fun inject(target: MediaPlayerControllerTest)

        fun inject(target: BookShelfTest)
    }

    private val rnd = Random()

    fun dummyBook(file1: File, file2: File): Book {
        val id = 1L
        val bookmarks = ArrayList<Bookmark>()
        val type = Book.Type.SINGLE_FILE
        val useCoverReplacement = false
        val author = "TestAuthor"
        val currentFile = file1
        val time = 0
        val name = "TestBook"
        val chapter1 = Chapter(file1, file1.name, 1 + rnd.nextInt(100000))
        val chapter2 = Chapter(file2, file2.name, 1 + rnd.nextInt(200000))
        val chapters = Lists.newArrayList(chapter1, chapter2)
        val playbackSpeed = 1F
        val root = Environment.getExternalStorageDirectory().path
        return Book(id,
                ImmutableList.copyOf(bookmarks),
                type, useCoverReplacement,
                author,
                currentFile,
                time,
                name,
                ImmutableList.copyOf(chapters),
                playbackSpeed,
                root)
    }
}