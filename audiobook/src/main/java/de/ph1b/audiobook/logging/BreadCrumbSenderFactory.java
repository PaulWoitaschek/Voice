package de.ph1b.audiobook.logging;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.sender.ReportSenderFactory;
import org.acra.util.JSONReportBuilder;

import timber.log.Timber;

/**
 * Created by ph1b on 12/03/16.
 */
public class BreadCrumbSenderFactory implements ReportSenderFactory {

    @NonNull
    @Override
    public ReportSender create(Context context, ACRAConfiguration config) {
        return new ReportSender() {
            @Override
            public void send(Context context, CrashReportData errorContent) throws ReportSenderException {
                try {
                    Timber.e("Timber caught %s", errorContent.toJSON().toString());
                } catch (JSONReportBuilder.JSONReportException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
