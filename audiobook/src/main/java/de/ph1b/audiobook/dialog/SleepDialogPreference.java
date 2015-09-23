package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ThemeUtil;

/**
 * Dialog for setting the sleep time.
 *
 * @author Paul Woitaschek
 */
public class SleepDialogPreference extends DialogPreference {


    private TextView timeView;

    public SleepDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_timer, null);
        final PrefsManager prefs = PrefsManager.getInstance(getContext());

        //init views
        timeView = (TextView) view.findViewById(R.id.minute_text);
        final NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.minute);
        ThemeUtil.theme(numberPicker);

        //init number picker
        int currentSleepValue = getSharedPreferences().getInt(getContext().getString(R.string.pref_key_sleep_time), 20);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(120);
        numberPicker.setValue(currentSleepValue);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateText(newVal);
            }
        });
        updateText(numberPicker.getValue());

        new MaterialDialog.Builder(getContext())
                .title(R.string.pref_sleep_time)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        int sleepAmount = numberPicker.getValue();
                        prefs.setSleepTime(sleepAmount);
                    }
                })
                .customView(view, true)
                .show();
    }

    private void updateText(int newVal) {
        timeView.setText(getContext().getResources().getQuantityString(R.plurals.pauses_after, newVal, newVal));
    }
}