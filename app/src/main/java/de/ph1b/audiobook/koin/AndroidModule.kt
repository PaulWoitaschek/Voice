package de.ph1b.audiobook.koin

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module

val AndroidModule = module {
  single { androidApplication().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
  single { androidApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager }
  single { androidApplication().getSystemService(Context.POWER_SERVICE) as PowerManager }
  single { androidApplication().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
  single { androidApplication().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
  single { androidApplication().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
}
