package de.ph1b.audiobook.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat

import com.afollestad.materialdialogs.MaterialDialog

import de.ph1b.audiobook.R

/**
 * Simple helper for obtaining android api 23 permissions

 * @author Paul Woitaschek
 */
object PermissionHelper {

    /**
     * This is a helper for [Activity.onRequestPermissionsResult] that
     * lets you determine if the permission granting worked.

     * @param receivedRequestCode The request code received
     * *
     * @param sentRequestCode     The request code expected
     * *
     * @param targetPermission    The permission to be obtained
     * *
     * @param permissions         The permissions that were checked
     * *
     * @param grantResults        The results for the permissions
     * *
     * @return true if permission granting worked
     */
    fun permissionGrantingWorked(receivedRequestCode: Int, sentRequestCode: Int, @SuppressWarnings("SameParameterValue") targetPermission: String, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (sentRequestCode == receivedRequestCode) {
            for (i in permissions.indices) {
                if (permissions[i] == targetPermission) {
                    return grantResults[i] == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        return false
    }

    /**
     * Shows a dialog that explains why the external storage permission is necessary and invokes
     * [ActivityCompat.requestPermissions] with the supplied request-
     * code on the activity on user confirm.

     * @param activity    The hosting dialog
     * *
     * @param requestCode The request code for [ActivityCompat.requestPermissions]
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun handleExtStorageRescan(activity: Activity, requestCode: Int) {
        val request = "${activity.getString(R.string.permission_read_ext_explanation)}\n\n${activity.getString(R.string.permission_read_ext_request)}"

        MaterialDialog.Builder(activity)
                .cancelable(false)
                .positiveText(R.string.permission_rescan)
                .onPositive { materialDialog, dialogAction ->
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            requestCode)
                }
                .content(request)
                .show()
    }
}
