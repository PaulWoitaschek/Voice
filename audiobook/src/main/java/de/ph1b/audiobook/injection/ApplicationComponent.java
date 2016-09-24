package de.ph1b.audiobook.injection;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.features.BaseActivity;
import de.ph1b.audiobook.features.BookActivity;
import de.ph1b.audiobook.features.book_overview.BookShelfAdapter;
import de.ph1b.audiobook.features.book_overview.BookShelfController;
import de.ph1b.audiobook.features.book_overview.BookShelfPresenter;
import de.ph1b.audiobook.features.book_overview.EditBookBottomSheet;
import de.ph1b.audiobook.features.book_overview.EditBookTitleDialogFragment;
import de.ph1b.audiobook.features.book_playing.BookPlayController;
import de.ph1b.audiobook.features.book_playing.JumpToPositionDialogFragment;
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment;
import de.ph1b.audiobook.features.book_playing.SleepTimerDialogFragment;
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment;
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity;
import de.ph1b.audiobook.features.folder_chooser.FolderChooserPresenter;
import de.ph1b.audiobook.features.folder_overview.FolderOverviewPresenter;
import de.ph1b.audiobook.features.imagepicker.ImagePickerController;
import de.ph1b.audiobook.features.settings.SettingsController;
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment;
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment;
import de.ph1b.audiobook.features.widget.WidgetUpdateService;
import de.ph1b.audiobook.playback.PlaybackService;
import de.ph1b.audiobook.playback.utils.ChangeNotifier;
import de.ph1b.audiobook.uitools.CoverReplacement;

/**
 * Base component that is the entry point for injection.
 *
 * @author Paul Woitaschek
 */
@SuppressWarnings("WeakerAccess")
@Singleton @Component(modules = {BaseModule.class, AndroidModule.class, PresenterModule.class, PrefsModule.class})
public interface ApplicationComponent {

   BookShelfPresenter getBookShelfPresenter();
   Context getContext();

   void inject(App target);
   void inject(AutoRewindDialogFragment target);
   void inject(BaseActivity target);
   void inject(PlaybackService target);
   void inject(BookActivity target);
   void inject(BookShelfAdapter target);
   void inject(BookmarkDialogFragment target);
   void inject(BookPlayController target);
   void inject(BookShelfController target);
   void inject(ChangeNotifier target);
   void inject(CoverReplacement target);
   void inject(SettingsController target);
   void inject(EditBookTitleDialogFragment target);
   void inject(EditBookBottomSheet target);
   void inject(FolderChooserActivity target);
   void inject(FolderChooserPresenter target);
   void inject(FolderOverviewPresenter target);
   void inject(ImagePickerController target);
   void inject(JumpToPositionDialogFragment target);
   void inject(PlaybackSpeedDialogFragment target);
   void inject(SeekDialogFragment target);
   void inject(SleepTimerDialogFragment target);
   void inject(ThemePickerDialogFragment target);
   void inject(WidgetUpdateService target);
}