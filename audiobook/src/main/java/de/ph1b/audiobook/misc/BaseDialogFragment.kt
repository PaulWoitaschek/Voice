package de.ph1b.audiobook.misc

import android.support.v4.app.DialogFragment

/**
 * Base DialogFragment
 *
 * @author Paul Woitaschek
 */
abstract class BaseDialogFragment : DialogFragment() {

  /** find a callback. The hosting activity must implement [RouterProvider] and the supplied key must match to the instance id of a controller */
  fun <T> findCallback(controllerBundleKey: String): T {
    val routerProvider = activity as RouterProvider
    val router = routerProvider.provideRouter()
    val controllerId: String = arguments.getString(controllerBundleKey)
    @Suppress("UNCHECKED_CAST")
    return router.getControllerWithInstanceId(controllerId) as T
  }
}