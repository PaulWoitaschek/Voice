package de.ph1b.audiobook.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.ph1b.audiobook.R;


public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
