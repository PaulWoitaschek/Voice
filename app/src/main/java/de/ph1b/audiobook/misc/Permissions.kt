package de.ph1b.audiobook.misc

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

/**
 * Class that simplifies the permission requesting
 */
class Permissions(private val activity: Activity) {

  private val REQUEST_CODE = 1512

  private val permissionSubject = PublishSubject.create<Array<String>>()

  fun request(permission: String): Single<PermissionResult> =
    if (hasPermission(permission)) Single.just(PermissionResult.GRANTED)
    else permissionSubject
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
          hasPermission(permission) -> PermissionResult.GRANTED
          showRationale(permission) -> PermissionResult.DENIED_ASK_AGAIN
          else -> PermissionResult.DENIED_FOREVER
        }
      }

  @Suppress("UNUSED_PARAMETER")
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_CODE) {
      permissionSubject.onNext(permissions)
    }
  }

  private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    activity,
    permission
  ) == PackageManager.PERMISSION_GRANTED

  private fun showRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

  enum class PermissionResult {
    GRANTED,
    DENIED_FOREVER,
    DENIED_ASK_AGAIN
  }
}
