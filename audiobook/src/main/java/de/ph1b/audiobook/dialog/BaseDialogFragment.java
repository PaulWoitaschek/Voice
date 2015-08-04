package de.ph1b.audiobook.dialog;

import android.support.v4.app.DialogFragment;

import de.ph1b.audiobook.utils.BaseApplication;

/**
 * Created by Paul Woitaschek (woitaschek@posteo.de, paul-woitaschek.de) on 04.08.15.
 * Simple base dialog fragment that is only watching for leaks
 */
public class BaseDialogFragment extends DialogFragment {

    @Override
    public void onDestroy() {
        super.onDestroy();

        BaseApplication.leakWatch(this);
    }
}
