package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.helper.CommonTasks;


public class Preferences extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_screen);
        new CommonTasks().checkExternalStorage(this);
    }

    @Override
    public void onResume(){
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }
}
