/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.injection;

import android.app.Application;
import android.content.Intent;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;

import javax.inject.Inject;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.logging.DelegateCrashesToTimber;
import de.ph1b.audiobook.logging.LogToStorageTree;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.persistence.LogStorage;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.playback.BookReaderService;
import timber.log.Timber;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        buildConfigClass = BuildConfig.class,
        formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "97user",
        formUriBasicAuthPassword = "sUjg9VkOgxTZbzVL")
public class App extends Application {

    private static ApplicationComponent applicationComponent;
    @Inject
    BookAdder bookAdder;
    @Inject
    PrefsManager prefsManager;

    public static ApplicationComponent component() {
        return applicationComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationComponent = newComponent();
        component().inject(this);

        ConfigurationBuilder acraBuilder = new ConfigurationBuilder(this);
        if (BuildConfig.DEBUG) {
            // init timber
            Timber.plant(new Timber.DebugTree());
            Timber.plant(new LogToStorageTree(LogStorage.INSTANCE));

            // force enable acra in debug mode
            prefsManager.setAcraEnabled(true);

            // forward crashes to timber
            //noinspection unchecked
            acraBuilder.setReportSenderFactoryClasses(new Class[]{DelegateCrashesToTimber.class});
        }

        // init acra and send breadcrumbs
        ACRA.init(this, acraBuilder.build());

        Timber.i("onCreate");

        bookAdder.scanForFiles(true);
        startService(new Intent(this, BookReaderService.class));
    }

    protected ApplicationComponent newComponent() {
        return DaggerApplicationComponent.builder()
                .androidModule(new AndroidModule(this))
                .build();
    }
}