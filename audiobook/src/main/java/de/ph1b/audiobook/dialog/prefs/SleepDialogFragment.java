package de.ph1b.audiobook.dialog.prefs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.SettingsSetListener;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;

/**
 * Dialog for setting the sleep time.
 *
 * @author Paul Woitaschek
 */
public class SleepDialogFragment extends DialogFragment {

    public static final String TAG = SleepDialogFragment.class.getSimpleName();


    @Bind(R.id.minute_text) TextView timeView;
    @Bind(R.id.minute) NumberPicker numberPicker;
    @Inject PrefsManager prefs;

    private SettingsSetListener settingsSetListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        settingsSetListener = (SettingsSetListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getComponent().inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_timer, null);
        ButterKnife.bind(this, view);

        //init views
        ThemeUtil.theme(numberPicker);

        //init number picker
        final int oldValue = prefs.getSleepTime();
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(120);
        numberPicker.setValue(oldValue);
        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateText(newVal));
        updateText(numberPicker.getValue());

        return new MaterialDialog.Builder(getContext())
                .title(R.string.pref_sleep_time)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive((materialDialog, dialogAction) -> {
                    int newValue = numberPicker.getValue();
                    prefs.setSleepTime(newValue);
                    settingsSetListener.onSettingsSet(newValue != oldValue);
                })
                .customView(view, true)
                .build();
    }

    private void updateText(int newVal) {
        timeView.setText(getContext().getResources().getQuantityString(R.plurals.pauses_after, newVal, newVal));
    }
}