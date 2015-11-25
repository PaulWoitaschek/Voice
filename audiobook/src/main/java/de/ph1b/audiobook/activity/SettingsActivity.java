package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.dialog.DonationDialogFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.interfaces.SettingsSetListener;

public class SettingsActivity extends BaseActivity implements DonationDialogFragment.OnDonationClickedListener,
        SettingsSetListener {

    @Bind(R.id.toolbar) Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
        ButterKnife.bind(this);

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
    public void onDonationClicked(@NonNull String item) {
        SettingsFragment settingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if (settingsFragment != null && settingsFragment.isVisible()) {
            settingsFragment.onDonationClicked(item);
        }
    }

    @Override
    public void onSettingsSet(boolean settingsChanged) {
        SettingsFragment settingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SettingsFragment.TAG);
        if (settingsFragment != null && settingsFragment.isVisible()) {
            settingsFragment.onSettingsSet(settingsChanged);
        }
    }
}
