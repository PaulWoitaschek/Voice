package de.ph1b.audiobook.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;

/**
 * Dialog explaining the user that it is necessary to have the permission. The hosting activity
 * must implement {@link de.ph1b.audiobook.dialog.ExplainReadExtStoragePermissionDialogFragment.RescanCallback}
 * <p/>
 * After interaction with the user is done, {@link RescanCallback#onRescan()} will be invoked.
 *
 * @author Paul Woitaschek
 */
public class ExplainReadExtStoragePermissionDialogFragment extends DialogFragment {

    public static final String TAG = ExplainReadExtStoragePermissionDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String request = getString(R.string.permission_read_ext_explanation) + "\n\n" + getString(R.string.permission_read_ext_request);
        return new MaterialDialog.Builder(getActivity())
                .cancelable(false)
                .positiveText(R.string.dialog_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ((RescanCallback) getActivity()).onRescan();
                    }
                })
                .content(request)
                .build();
    }


    public interface RescanCallback {
        /**
         * This method is called when a rescan should be made.
         */
        void onRescan();
    }
}
