package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Context
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import dagger.Component
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.activity.BaseActivity
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.activity.DependencyLicensesActivity
import de.ph1b.audiobook.activity.FolderOverviewActivity
import de.ph1b.audiobook.adapter.BookShelfAdapter
import de.ph1b.audiobook.adapter.BookmarkAdapter
import de.ph1b.audiobook.dialog.BookmarkDialogFragment
import de.ph1b.audiobook.dialog.EditCoverDialogFragment
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment
import de.ph1b.audiobook.dialog.SeekDialogFragment
import de.ph1b.audiobook.dialog.prefs.AutoRewindDialogFragment
import de.ph1b.audiobook.dialog.prefs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.dialog.prefs.SleepDialogFragment
import de.ph1b.audiobook.dialog.prefs.ThemePickerDialogFragment
import de.ph1b.audiobook.fragment.BookPlayFragment
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.fragment.SettingsFragment
import de.ph1b.audiobook.model.BookAdder
import de.ph1b.audiobook.service.BookReaderService
import de.ph1b.audiobook.service.WidgetUpdateService
import de.ph1b.audiobook.uitools.CoverReplacement
import org.acra.ACRA
import org.acra.annotation.ReportsCrashes
import org.acra.sender.HttpSender
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@ReportsCrashes(httpMethod = HttpSender.Method.PUT, reportType = HttpSender.Type.JSON, formUri = "http://acra-63e870.smileupps.com/acra-material/_design/acra-storage/_update/report", formUriBasicAuthLogin = "defaultreporter", formUriBasicAuthPassword = "KA0Kc8h4dV4lCZBz")
class App : Application() {

    @Inject internal lateinit var bookAdder: BookAdder

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            ACRA.init(this)
        }
        Timber.i("onCreate")
        refWatcher = LeakCanary.install(this)

        internalComponent = DaggerApp_ApplicationComponent.builder().baseModule(BaseModule()).androidModule(AndroidModule(this)).build()
        internalComponent.inject(this)

        bookAdder.scanForFiles(true)
    }

    @Singleton
    @Component(modules = arrayOf(BaseModule::class, AndroidModule::class))
    interface ApplicationComponent {

        val context: Context

        fun inject(target: WidgetUpdateService)

        fun inject(target: BookmarkAdapter)

        fun inject(target: CoverReplacement)

        fun inject(target: BaseActivity)

        fun inject(target: DependencyLicensesActivity)

        fun inject(target: ThemePickerDialogFragment)

        fun inject(target: SeekDialogFragment)

        fun inject(target: EditCoverDialogFragment)

        fun inject(target: JumpToPositionDialogFragment)

        fun inject(target: App)

        fun inject(target: BookReaderService)

        fun inject(target: SettingsFragment)

        fun inject(target: SleepDialogFragment)

        fun inject(target: PlaybackSpeedDialogFragment)

        fun inject(target: BookActivity)

        fun inject(target: BookPlayFragment)

        fun inject(target: BookmarkDialogFragment)

        fun inject(target: AutoRewindDialogFragment)

        fun inject(target: FolderOverviewActivity)

        fun inject(target: BookShelfAdapter)

        fun inject(target: BookShelfFragment)
    }

    companion object {

        private lateinit var internalComponent: ApplicationComponent
        fun component(): ApplicationComponent = internalComponent
        private var refWatcher: RefWatcher? = null

        fun leakWatch(`object`: Any) {
            refWatcher!!.watch(`object`)
        }
    }
}