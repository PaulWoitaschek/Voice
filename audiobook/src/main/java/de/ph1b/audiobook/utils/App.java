package de.ph1b.audiobook.utils;

import android.app.Application;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.dialog.AutoRewindDialogPreference;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditCoverDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.dialog.SeekDialogPreference;
import de.ph1b.audiobook.dialog.SleepDialogPreference;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.service.BookReaderService;
import de.ph1b.audiobook.service.WidgetUpdateService;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "defaultreporter",
        formUriBasicAuthPassword = "KA0Kc8h4dV4lCZBz")
public class App extends Application {

    private static ApplicationComponent applicationComponent;
    @Inject BookAdder bookAdder;

    public static ApplicationComponent getComponent() {
        return applicationComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }

        applicationComponent = DaggerApp_ApplicationComponent.builder().baseModule(new BaseModule(this)).build();
        applicationComponent.inject(this);


        bookAdder.scanForFiles(true);
    }

    @Singleton
    @Component(modules = BaseModule.class)
    public interface ApplicationComponent {
        void inject(WidgetUpdateService target);

        void inject(EditCoverDialogFragment target);

        void inject(JumpToPositionDialogFragment target);

        void inject(App target);

        void inject(BookReaderService target);

        void inject(SettingsFragment target);

        void inject(SleepDialogPreference target);

        void inject(PlaybackSpeedDialogFragment target);

        void inject(BookActivity target);

        void inject(BookPlayFragment target);

        void inject(SeekDialogPreference target);

        void inject(BookmarkDialogFragment target);

        void inject(AutoRewindDialogPreference target);

        void inject(FolderOverviewActivity target);

        void inject(BookShelfAdapter target);

        void inject(BookShelfFragment target);
    }
}