package de.ph1b.audiobook.injection;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.features.BaseActivity;
import de.ph1b.audiobook.features.BookActivity;
import de.ph1b.audiobook.features.BookAdder;
import de.ph1b.audiobook.features.book_overview.BookShelfAdapter;
import de.ph1b.audiobook.features.book_overview.BookShelfBasePresenter;
import de.ph1b.audiobook.features.book_overview.BookShelfFragment;
import de.ph1b.audiobook.features.book_overview.BookShelfPresenter;
import de.ph1b.audiobook.features.book_overview.EditBookTitleDialogFragment;
import de.ph1b.audiobook.features.book_playing.BookPlayFragment;
import de.ph1b.audiobook.features.book_playing.JumpToPositionDialogFragment;
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment;
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment;
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity;
import de.ph1b.audiobook.features.folder_chooser.FolderChooserPresenter;
import de.ph1b.audiobook.features.folder_overview.FolderOverviewActivity;
import de.ph1b.audiobook.features.folder_overview.FolderOverviewPresenter;
import de.ph1b.audiobook.features.imagepicker.ImagePickerActivity;
import de.ph1b.audiobook.features.settings.SettingsFragment;
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment;
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.features.settings.dialogs.SleepDialogFragment;
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment;
import de.ph1b.audiobook.features.widget.WidgetUpdateService;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.BookReaderService;
import de.ph1b.audiobook.playback.PlayStateManager;
import de.ph1b.audiobook.uitools.CoverReplacement;

/**
 * Base component that is the entry point for injection.
 *
 * @author Paul Woitaschek
 */
@Singleton
@Component(modules = {BaseModule.class, AndroidModule.class, PresenterModule.class})
public interface ApplicationComponent {

    BookShelfBasePresenter getBookShelfBasePresenter();

    Context getContext();

    PrefsManager getPrefsManager();

    BookAdder getBookAdder();

    PlayStateManager playStateManager();

    void inject(WidgetUpdateService target);

    void inject(EditBookTitleDialogFragment target);

    void inject(CoverReplacement target);

    void inject(BaseActivity target);

    void inject(FolderChooserActivity target);

    void inject(ThemePickerDialogFragment target);

    void inject(SeekDialogFragment target);

    void inject(ImagePickerActivity target);

    void inject(JumpToPositionDialogFragment target);

    void inject(App target);

    void inject(BookReaderService target);

    void inject(SettingsFragment target);

    void inject(SleepDialogFragment target);

    void inject(PlaybackSpeedDialogFragment target);

    void inject(BookActivity target);

    void inject(FolderChooserPresenter target);

    void inject(BookPlayFragment target);

    void inject(BookmarkDialogFragment target);

    void inject(AutoRewindDialogFragment target);

    void inject(FolderOverviewActivity target);

    void inject(BookShelfAdapter target);

    void inject(BookShelfFragment target);

    void inject(BookShelfPresenter target);

    void inject(FolderOverviewPresenter target);
}
