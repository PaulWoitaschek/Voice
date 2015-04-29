package de.ph1b.audiobook.utils;

import android.app.Application;

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


    @Override
    public void onCreate() {
        super.onCreate();

        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }

        BookAdder.getInstance(this).scanForFiles(true);
    }
}