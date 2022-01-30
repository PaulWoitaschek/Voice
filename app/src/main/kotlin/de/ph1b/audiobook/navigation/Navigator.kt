package de.ph1b.audiobook.navigation

import android.app.Activity
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.misc.conductor.asTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigator
@Inject constructor() {

  private var router: Router? = null
  private var currentActivity: Activity? = null

  fun push(controller: Controller) {
    val router = router ?: return
    if (controller is DialogController) {
      controller.showDialog(router)
    } else {
      router.pushController(controller.asTransaction())
    }
  }

  fun setRoutingComponents(activity: Activity, router: Router) {
    currentActivity = activity
    this.router = router
  }

  fun clear(activity: Activity) {
    if (currentActivity == activity) {
      router = null
      currentActivity = null
    }
  }
}
