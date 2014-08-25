package de.ph1b.audiobook.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.ph1b.audiobook.R;


public class PreferencesFragment extends PreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
