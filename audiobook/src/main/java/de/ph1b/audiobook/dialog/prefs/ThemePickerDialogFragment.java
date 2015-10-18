package de.ph1b.audiobook.dialog.prefs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.SettingsSetListener;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;

/**
 * Dialog for picking the UI theme.
 *
 * @author Paul Woitaschek
 */
public class ThemePickerDialogFragment extends DialogFragment {

    public static final String TAG = ThemePickerDialogFragment.class.getSimpleName();
    @Inject PrefsManager prefsManager;
    private SettingsSetListener settingsSetListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getComponent().inject(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        settingsSetListener = (SettingsSetListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ThemeUtil.Theme oldTheme = prefsManager.getTheme();
        final List<ThemeUtil.Theme> existingThemes = Arrays.asList(ThemeUtil.Theme.values());
        List<String> names = new ArrayList<>(existingThemes.size());
        for (ThemeUtil.Theme t : ThemeUtil.Theme.values()) {
            names.add(getString(t.getNameId()));
        }

        return new MaterialDialog.Builder(getContext())
                .items(names.toArray(new CharSequence[names.size()]))
                .itemsCallbackSingleChoice(existingThemes.indexOf(oldTheme), new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        ThemeUtil.Theme newTheme = existingThemes.get(i);
                        prefsManager.setTheme(newTheme);
                        settingsSetListener.onSettingsSet(newTheme != oldTheme);
                        return true;
                    }
                })
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .title(R.string.pref_theme_title)
                .build();
    }
}
