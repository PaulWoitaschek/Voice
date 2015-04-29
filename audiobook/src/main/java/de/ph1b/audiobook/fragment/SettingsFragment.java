package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.dialog.AutoRewindDialogPreference;
import de.ph1b.audiobook.dialog.DonationDialogFragment;
import de.ph1b.audiobook.dialog.SeekDialogPreference;
import de.ph1b.audiobook.dialog.SleepDialogPreference;
import de.ph1b.audiobook.dialog.SupportDialogFragment;
import de.ph1b.audiobook.utils.PrefsManager;
import de.ph1b.audiobook.vendinghelper.IabHelper;
import de.ph1b.audiobook.vendinghelper.IabResult;
import de.ph1b.audiobook.vendinghelper.Purchase;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.
        OnSharedPreferenceChangeListener, DonationDialogFragment.OnDonationClickedListener {

    public static final String TAG = SettingsFragment.class.getSimpleName();
    private PrefsManager prefs;
    private SharedPreferences sp;
    private boolean donationAvailable = false;
    private IabHelper iabHelper;

    @Override
    public void onDestroy() {
        super.onDestroy();

        iabHelper.dispose();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        prefs = new PrefsManager(getActivity());
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
        initValues();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    private void initValues() {
        // seek pref
        int seekAmount = prefs.getSeekTime();
        String seekSummary = seekAmount + " " + getString(R.string.seconds);
        SeekDialogPreference seekDialogPreference = (SeekDialogPreference) findPreference(getString(R.string.pref_key_seek_time));
        seekDialogPreference.setSummary(seekSummary);

        // auto rewind pref
        int autoRewindAmount = prefs.getAutoRewindAmount();
        String autoRewindSummary = autoRewindAmount + " " + getString(R.string.seconds);
        AutoRewindDialogPreference autoRewindDialogPreference = (AutoRewindDialogPreference) findPreference(getString(R.string.pref_key_auto_rewind));
        autoRewindDialogPreference.setSummary(autoRewindSummary);

        // sleep pref
        int sleepAmount = prefs.getSleepTime();
        String sleepSummary = String.valueOf(sleepAmount) + " " + getString(R.string.minutes);
        SleepDialogPreference sleepDialogPreference = (SleepDialogPreference) findPreference(getString(R.string.pref_key_sleep_time));
        sleepDialogPreference.setSummary(sleepSummary);

        // folder pref
        Preference folderPreference = findPreference(getString(R.string.pref_key_audiobook_folders));
        folderPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), FolderOverviewActivity.class));
                return true;
            }
        });

        // theme pref
        MaterialListPreference themePreference = (MaterialListPreference) findPreference(getString(R.string.pref_key_theme));
        String theme = sp.getString(getString(R.string.pref_key_theme), "light");
        String themeSummary;
        switch (theme) {
            case "light":
                themeSummary = getString(R.string.pref_theme_light);
                break;
            case "dark":
                themeSummary = getString(R.string.pref_theme_dark);
                break;
            default:
                throw new AssertionError("This should not have happened. There is no theme for key=" + theme);
        }
        themePreference.setSummary(themeSummary);

        BaseActivity baseActivity = (BaseActivity) getActivity();
        baseActivity.recreateIfThemeChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        initValues();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_contribute:
                new SupportDialogFragment().show(getFragmentManager(), SupportDialogFragment.TAG);
                return true;
            default:
                return false;
        }
    }

    public void onDonationClicked(String item) {
        if (donationAvailable) {
            iabHelper.launchPurchaseFlow(getActivity(), item, 10001,
                    new IabHelper.OnIabPurchaseFinishedListener() {
                        @Override
                        public void onIabPurchaseFinished(IabResult result, Purchase info) {
                            String message;
                            if (result.isSuccess()) {
                                message = getString(R.string.donation_worked_thanks);
                            } else {
                                message = getString(R.string.donation_not_worked) + ":\n"
                                        + result.getMessage();
                            }
                            Toast.makeText(getActivity(), message,
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
}