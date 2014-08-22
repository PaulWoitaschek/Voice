package de.ph1b.audiobook.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.app.ActionBarActivity;


import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;


public class Preferences extends PreferenceFragment {

    public static final String TAG = "de.ph1b.audiobook.fragment.Preferences";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.preferences);
        String actionBarTitle = getString(R.string.action_settings);
        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(actionBarTitle);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null)
            v.setBackgroundColor(getResources().getColor(R.color.fragment_background));
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onCreateOptionsMenu was called!");
        inflater.inflate(R.menu.empty, menu);
    }
}
