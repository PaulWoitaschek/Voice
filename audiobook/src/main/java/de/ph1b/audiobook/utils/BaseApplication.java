package de.ph1b.audiobook.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.support.v4.app.Fragment;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.model.BookAdder;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "defaultreporter",
        formUriBasicAuthPassword = "KA0Kc8h4dV4lCZBz")
public class BaseApplication extends Application {

    private RefWatcher refWatcher;

    private static RefWatcher getRefWatcher(Context context) {
        BaseApplication application = (BaseApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    public static void leakWatch(Fragment fragment) {
        leakWatch(fragment.getActivity());
    }

    public static void leakWatch(android.app.Fragment fragment) {
        leakWatch(fragment.getActivity());
    }

    public static void leakWatch(Activity activity) {
        getRefWatcher(activity).watch(activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        refWatcher = LeakCanary.install(this);


        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        } else {
            ACRA.init(this);
        }

        BookAdder.getInstance(this).scanForFiles(true);
    }
}