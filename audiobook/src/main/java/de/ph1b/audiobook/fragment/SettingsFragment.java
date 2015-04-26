package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BaseActivity;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.dialog.AutoRewindDialogPreference;
import de.ph1b.audiobook.dialog.SeekDialogPreference;
import de.ph1b.audiobook.dialog.SleepDialogPreference;
import de.ph1b.audiobook.utils.PrefsManager;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private PrefsManager prefs;
    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsManager(getActivity());
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
}