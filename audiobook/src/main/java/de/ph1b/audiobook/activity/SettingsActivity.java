package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.SettingsFragment;

public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }
}
