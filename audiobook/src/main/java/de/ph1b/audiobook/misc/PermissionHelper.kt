package de.ph1b.audiobook.misc

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import de.ph1b.audiobook.R
import de.ph1b.audiobook.uitools.BetterSnack
import permissions.dispatcher.PermissionRequest

/**
 * Simple helper for obtaining android api 23 permissions

 * @author Paul Woitaschek
 */
object PermissionHelper {

    const val NEEDED_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE


    fun showRationaleAndProceed(root: View, request: PermissionRequest) {
        BetterSnack.make(
                root = root,
                text = root.context.getString(R.string.permission_external_new_explanation),
                action = root.context.getString(R.string.permission_retry)) {
            request.proceed()
        }
    }

    fun handleDeniedForever(root:View){
        val context = root.context
        BetterSnack.make(
                root = root,
                text = context.getString(R.string.permission_external_new_explanation),
                action = context.getString(R.string.permission_goto_settings)) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
    }
}
