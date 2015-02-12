package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;
import de.ph1b.audiobook.dialog.SeekPreferenceDialog;
import de.ph1b.audiobook.dialog.SleepPreferenceDialog;
import de.ph1b.audiobook.utils.PrefsManager;


public class PreferencesFragment extends PreferenceFragment {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            initValues();
        }
    };
    private PrefsManager prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        prefs = new PrefsManager(getActivity());
    }

    private void initValues() {
        int seekAmount = prefs.getSeekTime();
        String seekSummary = String.valueOf(seekAmount) + " " + getString(R.string.seconds);
        SeekPreferenceDialog seekPreferenceDialog = (SeekPreferenceDialog) findPreference(getString(R.string.pref_key_seek_time));
        seekPreferenceDialog.setSummary(seekSummary);

        int sleepAmount = prefs.getSleepTime();
        String sleepSummary = String.valueOf(sleepAmount) + " " + getString(R.string.minutes);
        SleepPreferenceDialog sleepPreferenceDialog = (SleepPreferenceDialog) findPreference(getString(R.string.pref_key_sleep_time));
        sleepPreferenceDialog.setSummary(sleepSummary);

        Preference folderPreference = findPreference(getString(R.string.pref_key_root_folder));
        if (folderPreference != null) {
            String preferenceSummary = new PrefsManager(getActivity()).getAudiobookFolder();
            if (preferenceSummary != null) {
                folderPreference.setSummary(preferenceSummary);
            }
            folderPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), FolderChooserActivity.class));
                    return true;
                }
            });

            // TODO: Remove this line when screen is needed
            getPreferenceScreen().removePreference(folderPreference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initValues();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }
}
