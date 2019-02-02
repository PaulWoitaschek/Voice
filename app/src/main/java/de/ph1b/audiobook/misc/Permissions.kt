package de.ph1b.audiobook.misc

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

private const val REQUEST_CODE = 1512

/**
 * Class that simplifies the permission requesting
 */
class Permissions(private val activity: Activity) {

  private val permissionSubject = PublishSubject.create<Array<String>>()

  fun request(permission: String): Single<PermissionResult> {
    return if (activity.hasPermission(permission)) {
      Single.just(PermissionResult.GRANTED)
    } else {
      permissionSubject
        .doOnSubscribe {
          ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            REQUEST_CODE
          )
        }
        .filter { it.contains(permission) }
        .firstOrError()
        .map {
          when {
            activity.hasPermission(permission) -> PermissionResult.GRANTED
            showRationale(permission) -> PermissionResult.DENIED_ASK_AGAIN
            else -> PermissionResult.DENIED_FOREVER
          }
        }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == REQUEST_CODE) {
      permissionSubject.onNext(permissions)
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
