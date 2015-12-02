package de.ph1b.audiobook.testing

import android.app.Application
import android.content.Context
import dagger.Component
import de.ph1b.audiobook.injection.AndroidModule
import de.ph1b.audiobook.injection.BaseModule
import de.ph1b.audiobook.mediaplayer.MediaPlayerControllerTest
import javax.inject.Singleton

/**
 * Providing mocked components with mocked modules
 *
 * @author Paul Woitaschek
 */
class MockProvider(context: Context) {

    val mockAppComponent: MockComponent = DaggerMockProvider_MockAppComponent.builder()
            .androidModule(AndroidModule(context.applicationContext as Application))
            .baseModule(BaseModule())
            .build()

    @Singleton
    @Component(modules = arrayOf(BaseModule::class, AndroidModule::class))
    interface MockComponent {

        fun inject(target: MediaPlayerControllerTest)
    }
}