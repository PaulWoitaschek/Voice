package de.ph1b.audiobook

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.rules.ExternalResource


class RxMainThreadTrampolineRule : ExternalResource() {

  override fun before() {
    super.before()
    RxAndroidPlugins.initMainThreadScheduler { Schedulers.trampoline() }
  }

  override fun after() {
    super.after()
    RxAndroidPlugins.reset()
  }
}
