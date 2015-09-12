package de.ph1b.audiobook.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;

/**
 * Simple helper for obtaining android api 23 permissions
 *
 * @author Paul Woitaschek
 */
public class PermissionHelper {

    /**
     * This is a helper for {@link Activity#onRequestPermissionsResult(int, String[], int[])} that
     * lets you determine if the permission granting worked.
     *
     * @param receivedRequestCode The request code received
     * @param sentRequestCode     The request code expected
     * @param targetPermission    The permission to be obtained
     * @param permissions         The permissions that were checked
     * @param grantResults        The results for the permissions
     * @return true if permission granting worked
     */
    public static boolean permissionGrantingWorked(int receivedRequestCode, int sentRequestCode, String targetPermission, String[] permissions, int[] grantResults) {
        if (sentRequestCode == receivedRequestCode) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(targetPermission)) {
                    return grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }
        }
        return false;
    }

    /**
     * Shows a dialog that explains why the external storage permission is necessary and invokes
     * {@link ActivityCompat#requestPermissions(Activity, String[], int)} with the supplied request-
     * code on the activity on user confirm.
     *
     * @param activity    The hosting dialog
     * @param requestCode The request code for {@link ActivityCompat#requestPermissions(Activity, String[], int)}
     */
    public static void handleExtStorageRescan(final Activity activity, final int requestCode) {
        String request = activity.getString(R.string.permission_read_ext_explanation) + "\n\n" +
                activity.getString(R.string.permission_read_ext_request);
        new MaterialDialog.Builder(activity)
                .cancelable(false)
                .positiveText(R.string.permission_rescan)
                .callback(new MaterialDialog.ButtonCallback() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                requestCode);
                    }
                })
                .content(request)
                .show();
    }
}
