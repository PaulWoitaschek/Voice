package de.ph1b.audiobook.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import org.videolan.libvlc.LibVLC;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.dialog.SeekPreferenceDialog;
import de.ph1b.audiobook.dialog.SleepPreferenceDialog;


public class PreferencesFragment extends PreferenceFragment {


    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            initValues();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initValues() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int seekAmount = sp.getInt(getString(R.string.pref_key_seek_time), 20);
        String seekSummary = String.valueOf(seekAmount) + " " + getString(R.string.seconds);
        SeekPreferenceDialog seekPreferenceDialog = (SeekPreferenceDialog) findPreference(getString(R.string.pref_key_seek_time));
        seekPreferenceDialog.setSummary(seekSummary);

        int sleepAmount = sp.getInt(getString(R.string.pref_key_sleep_time), 20);
        String sleepSummary = String.valueOf(sleepAmount) + " " + getString(R.string.minutes);
        SleepPreferenceDialog sleepPreferenceDialog = (SleepPreferenceDialog) findPreference(getString(R.string.pref_key_sleep_time));
        sleepPreferenceDialog.setSummary(sleepSummary);

        int hwAcceleration = Integer.valueOf(sp.getString(getString(R.string.pref_key_hardware_acceleration), String.valueOf(LibVLC.HW_ACCELERATION_FULL)));
        ListPreference hardwareAccelerationDialog = (ListPreference) findPreference(getString(R.string.pref_key_hardware_acceleration));
        String[] values = getResources().getStringArray(R.array.hardware_acceleration_list);
        String hwAccelerationSummary = "";
        switch (hwAcceleration) {
            case LibVLC.HW_ACCELERATION_AUTOMATIC:
                hwAccelerationSummary = values[0];
                break;
            case LibVLC.HW_ACCELERATION_DISABLED:
                hwAccelerationSummary = values[1];
                break;
            case LibVLC.HW_ACCELERATION_DECODING:
                hwAccelerationSummary = values[2];
                break;
            case LibVLC.HW_ACCELERATION_FULL:
                hwAccelerationSummary = values[3];
                break;
        }
        hardwareAccelerationDialog.setSummary(hwAccelerationSummary);
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
