package voice.app.navigation

import android.app.Activity
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import voice.app.misc.conductor.asTransaction
import voice.common.conductor.DialogController
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
