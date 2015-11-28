package de.ph1b.audiobook.injection;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Component;
import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.adapter.BookmarkAdapter;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditCoverDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.SeekDialogFragment;
import de.ph1b.audiobook.dialog.prefs.AutoRewindDialogFragment;
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.dialog.prefs.SleepDialogFragment;
import de.ph1b.audiobook.dialog.prefs.ThemePickerDialogFragment;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.service.BookReaderService;
import de.ph1b.audiobook.service.WidgetUpdateService;
import de.ph1b.audiobook.uitools.CoverReplacement;
import timber.log.Timber;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "defaultreporter",
        formUriBasicAuthPassword = "KA0Kc8h4dV4lCZBz")
public class App extends Application {

    private static ApplicationComponent applicationComponent;
    private static RefWatcher refWatcher;
    @Inject BookAdder bookAdder;

    public static void leakWatch(Object object) {
        refWatcher.watch(object);
    }

    public static ApplicationComponent getComponent() {
        return applicationComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            ACRA.init(this);
        }
        Timber.i("onCreate");
        refWatcher = LeakCanary.install(this);

        applicationComponent = DaggerApp_ApplicationComponent.builder()
                .baseModule(new BaseModule())
                .androidModule(new AndroidModule(this))
                .build();
        applicationComponent.inject(this);

        bookAdder.scanForFiles(true);
    }

    @Singleton
    @Component(modules = {BaseModule.class, AndroidModule.class})
    public interface ApplicationComponent {

        Context getContext();

        void inject(WidgetUpdateService target);

        void inject(BookmarkAdapter target);

        void inject(CoverReplacement target);

        void inject(BaseActivity target);

        void inject(ThemePickerDialogFragment target);

        void inject(SeekDialogFragment target);

        void inject(EditCoverDialogFragment target);

        void inject(JumpToPositionDialogFragment target);

        void inject(App target);

        void inject(BookReaderService target);

        void inject(SettingsFragment target);

        void inject(SleepDialogFragment target);

        void inject(PlaybackSpeedDialogFragment target);

        void inject(BookActivity target);

        void inject(BookPlayFragment target);

        void inject(BookmarkDialogFragment target);

        void inject(AutoRewindDialogFragment target);

        void inject(FolderOverviewActivity target);

        void inject(BookShelfAdapter target);

        void inject(BookShelfFragment target);
    }
}