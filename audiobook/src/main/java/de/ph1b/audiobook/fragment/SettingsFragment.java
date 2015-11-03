package de.ph1b.audiobook.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import javax.inject.Inject;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.dialog.DonationDialogFragment;
import de.ph1b.audiobook.dialog.SeekDialogFragment;
import de.ph1b.audiobook.dialog.SupportDialogFragment;
import de.ph1b.audiobook.dialog.prefs.AutoRewindDialogFragment;
import de.ph1b.audiobook.dialog.prefs.SleepDialogFragment;
import de.ph1b.audiobook.dialog.prefs.ThemePickerDialogFragment;
import de.ph1b.audiobook.interfaces.SettingsSetListener;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.vendinghelper.IabHelper;
import de.ph1b.audiobook.vendinghelper.IabResult;
import timber.log.Timber;

public class SettingsFragment extends PreferenceFragment implements DonationDialogFragment.OnDonationClickedListener,
        SettingsSetListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();
    private final Handler handler = new Handler();
    @Inject PrefsManager prefs;
    private Preference themePreference;
    private Preference sleepPreference;
    private Preference seekPreference;
    private Preference autoRewindPreference;
    private boolean donationAvailable = false;
    private IabHelper iabHelper;
    private BaseActivity hostingActivity;

    @Override
    public void onDestroy() {
        super.onDestroy();

        iabHelper.dispose();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = hostingActivity.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        updateValues();

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getComponent().inject(this);

        addPreferencesFromResource(R.xml.preferences);

        setHasOptionsMenu(true);

        iabHelper = new IabHelper(getActivity(), "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApfo7lNYf9Mh" +
                "GiHAZO8iG/LX3SDGg7Gv7s41FEf08rxCuIuE+6QdQ0u+yZEoirislWV7jMqHY3XlyJMrH+/nKqrtYgw" +
                "qnFtwuwckS/5R+0dtSKL4F/aVm6a3p00BtCjqe7tXrEg90gpVk59p5qr1cOnOAAc/xmerFG9VCv8QHw" +
                "I9arlShCcXz7eTKemxjkHMO3dTkTKDjYZMIozr0t9qTvTxPz1aV6TWAGs5E6Dt7UF78pntgG9bMwmIgL" +
                "N6fOYuBaKd8IxA3iQ5IhWGVB8WG65Ax+u0RXsx0r8BC53JQq91lItka7b1OeBe6uPHeqk8IQWY0l57AW" +
                "fjZOFlNyWQB4QIDAQAB");
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                donationAvailable = result.isSuccess();
            }
        });

        // seek pref
        seekPreference = findPreference(getString(R.string.pref_key_seek_time));
        seekPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new SeekDialogFragment().show(hostingActivity.getSupportFragmentManager(), SeekDialogFragment.TAG);
                return true;
            }
        });

        // auto rewind pref
        autoRewindPreference = findPreference(getString(R.string.pref_key_auto_rewind));
        autoRewindPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AutoRewindDialogFragment().show(hostingActivity.getSupportFragmentManager(), AutoRewindDialogFragment.TAG);
                return true;
            }
        });

        // sleep pref
        sleepPreference = findPreference(getString(R.string.pref_key_sleep_time));
        sleepPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new SleepDialogFragment().show(hostingActivity.getSupportFragmentManager(), SleepDialogFragment.TAG);
                return true;
            }
        });

        // folder pref
        Preference folderPreference = findPreference(getString(R.string.pref_key_audiobook_folders));
        folderPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(hostingActivity, FolderOverviewActivity.class));
                return true;
            }
        });

        // theme pref
        themePreference = findPreference(getString(R.string.pref_key_theme));
        themePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new ThemePickerDialogFragment().show(hostingActivity.getSupportFragmentManager(), ThemePickerDialogFragment.TAG);
                return true;
            }
        });
    }

    private void updateValues() {
        ThemeUtil.Theme theme = prefs.getTheme();
        themePreference.setSummary(theme.getNameId());

        int sleepAmount = prefs.getSleepTime();
        String sleepSummary = getResources().getQuantityString(R.plurals.minutes, sleepAmount, sleepAmount);
        sleepPreference.setSummary(sleepSummary);

        int autoRewindAmount = prefs.getAutoRewindAmount();
        String autoRewindSummary = getResources().getQuantityString(R.plurals.seconds, autoRewindAmount, autoRewindAmount);
        autoRewindPreference.setSummary(autoRewindSummary);

        int seekAmount = prefs.getSeekTime();
        seekPreference.setSummary(getResources().getQuantityString(R.plurals.seconds, seekAmount, seekAmount));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        //noinspection deprecation
        super.onAttach(activity);

        hostingActivity = (BaseActivity) activity;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_contribute:
                new SupportDialogFragment().show(hostingActivity.getSupportFragmentManager(),
                        SupportDialogFragment.TAG);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDonationClicked(String item) {
        Timber.d("onDonationClicked with item=%s and donationAvailable=%b", item, donationAvailable);
        if (donationAvailable) {
            iabHelper.launchPurchaseFlow(hostingActivity, item,
                    new IabHelper.OnIabPurchaseFinishedListener() {
                        @Override
                        public void onIabPurchaseFinished(IabResult result) {
                            String message;
                            if (result.isSuccess()) {
                                message = getString(R.string.donation_worked_thanks);
                            } else {
                                message = getString(R.string.donation_not_worked) + ":\n"
                                        + result.getMessage();
                            }
                            Toast.makeText(hostingActivity, message,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        iabHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSettingsSet(boolean settingsChanged) {
        if (settingsChanged) {
            updateValues();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    hostingActivity.recreateIfThemeChanged();
                }
            });
        }
    }
}