package de.ph1b.audiobook.fragment;

import android.support.v4.app.Fragment;

import de.ph1b.audiobook.utils.App;

/**
 * Base fragment all fragments should inherit from. Handles memory leak detection.
 *
 * @author Paul Woitaschek
 */
public class BaseFragment extends Fragment {

    @Override
    public void onDestroy() {
        super.onDestroy();

        App.leakWatch(this);
    }
}
