package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.dialog.DonationDialogFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;

public class SettingsActivity extends BaseActivity implements DonationDialogFragment.OnDonationClickedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment(), SettingsFragment.TAG)
                    .commit();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SettingsFragment settingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if (settingsFragment != null && settingsFragment.isVisible()) {
            settingsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDonationClicked(String item) {
        SettingsFragment settingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if (settingsFragment != null && settingsFragment.isVisible()) {
            settingsFragment.onDonationClicked(item);
        }
    }
}
