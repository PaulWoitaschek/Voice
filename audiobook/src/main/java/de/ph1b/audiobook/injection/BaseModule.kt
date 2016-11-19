package de.ph1b.audiobook.injection

import android.content.Context
import com.tbruyelle.rxpermissions.RxPermissions
import dagger.Module
import dagger.Provides
import dagger.Reusable

/**
 * Basic providing module.
 *
 * @author Paul Woitaschek
 */
@Module class BaseModule {

  @Provides @Reusable fun provideRxPermissions(context: Context): RxPermissions = RxPermissions.getInstance(context)
}
