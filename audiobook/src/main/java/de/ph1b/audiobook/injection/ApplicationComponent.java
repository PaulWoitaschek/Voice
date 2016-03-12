package de.ph1b.audiobook.injection;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.activity.DependencyLicensesActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditBookTitleDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.SeekDialogFragment;
import de.ph1b.audiobook.dialog.prefs.AutoRewindDialogFragment;
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.dialog.prefs.SleepDialogFragment;
import de.ph1b.audiobook.dialog.prefs.ThemePickerDialogFragment;
import de.ph1b.audiobook.features.imagepicker.ImagePickerActivity;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.BookReaderService;
import de.ph1b.audiobook.playback.PlayStateManager;
import de.ph1b.audiobook.playback.WidgetUpdateService;
import de.ph1b.audiobook.presenter.BookShelfBasePresenter;
import de.ph1b.audiobook.presenter.BookShelfPresenter;
import de.ph1b.audiobook.presenter.FolderChooserPresenter;
import de.ph1b.audiobook.presenter.FolderOverviewPresenter;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.view.FolderChooserActivity;
import de.ph1b.audiobook.view.FolderOverviewActivity;
import de.ph1b.audiobook.view.fragment.BookShelfFragment;

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

    void inject(DependencyLicensesActivity target);

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
