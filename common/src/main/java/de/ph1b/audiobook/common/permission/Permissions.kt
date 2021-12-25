package de.ph1b.audiobook.common.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

private const val REQUEST_CODE = 1512

/**
 * Class that simplifies the permission requesting
 */
class Permissions(private val activity: Activity) {

  private val permissionChannel = BroadcastChannel<Array<String>>(1)

  suspend fun request(permission: String): PermissionResult {
    return if (activity.hasPermission(permission)) {
      PermissionResult.GRANTED
    } else {
      permissionChannel.asFlow()
        .onStart {
          ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            REQUEST_CODE
          )
        }
        .filter { it.contains(permission) }
        .first()

      when {
        activity.hasPermission(permission) -> PermissionResult.GRANTED
        showRationale(permission) -> PermissionResult.DENIED_ASK_AGAIN
        else -> PermissionResult.DENIED_FOREVER
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == REQUEST_CODE) {
      permissionChannel.trySend(permissions)
    }
  }

  private fun showRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
  }

  enum class PermissionResult {
    GRANTED,
    DENIED_FOREVER,
    DENIED_ASK_AGAIN
  }
}

fun Context.hasPermission(permission: String): Boolean {
  val permissionResult = ContextCompat.checkSelfPermission(this, permission)
  return permissionResult == PackageManager.PERMISSION_GRANTED
}
