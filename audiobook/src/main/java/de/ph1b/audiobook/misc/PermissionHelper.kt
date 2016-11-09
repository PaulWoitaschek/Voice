package de.ph1b.audiobook.misc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import com.tbruyelle.rxpermissions.RxPermissions
import de.ph1b.audiobook.R
import de.ph1b.audiobook.uitools.BetterSnack
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Simple helper for obtaining android api 23 permissions
 *
 * @author Paul Woitaschek
 */
class PermissionHelper
@Inject constructor(private val rxPermissions: RxPermissions) {

    private val PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val permissionDialogConfirmed = PublishSubject.create<Unit>()

    fun storagePermission(activity: Activity, gotPermission: () -> Unit = {}) {
        val root = activity.findViewById(android.R.id.content)
        rxPermissions.request(PERMISSION).toV2Observable()
                .repeatWhen { permissionDialogConfirmed }
                .flatMap { granted ->
                    if (granted) {
                        gotPermission()
                        Observable.just(true)
                    } else {
                        rxPermissions.shouldShowRequestPermissionRationale(activity, PERMISSION).toV2Observable()
                                .doOnNext { showRationale ->
                                    if (showRationale) {
                                        showRationale(root) {
                                            permissionDialogConfirmed.onNext(Unit)
                                        }
                                    } else handleDeniedForever(root)
                                }
                    }
                }.subscribe()
    }


    private fun showRationale(root: View, listener: () -> Unit) {
        BetterSnack.make(
                root = root,
                text = root.context.getString(R.string.permission_external_new_explanation),
                action = root.context.getString(R.string.permission_retry),
                listener = listener)
    }

    private fun handleDeniedForever(root: View) {
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
