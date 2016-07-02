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
 * A simple factory that delegates crashes to timber instead of sending them to the backend
 *
 * @author Paul Woitaschek
 */
public class DelegateCrashesToTimber implements ReportSenderFactory {

   @NonNull @Override
   public ReportSender create(@NonNull Context context, @NonNull ACRAConfiguration config) {
      return new ReportSender() {
         @Override public void send(
               @NonNull Context context,
               @NonNull CrashReportData errorContent) throws ReportSenderException {
            try {
               Timber.e("Timber caught %s", errorContent.toJSON().toString());
            } catch (JSONReportBuilder.JSONReportException e) {
               e.printStackTrace();
            }
         }
      };
   }
}