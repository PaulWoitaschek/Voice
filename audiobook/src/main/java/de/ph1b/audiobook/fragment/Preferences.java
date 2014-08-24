package de.ph1b.audiobook.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onOptionsItemSelected was called with: " + item.toString());
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new BookChoose())
                        .commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
