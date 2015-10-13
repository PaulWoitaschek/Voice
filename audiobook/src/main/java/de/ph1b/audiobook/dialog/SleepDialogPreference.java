package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;

/**
 * Dialog for setting the sleep time.
 *
 * @author Paul Woitaschek
 */
public class SleepDialogPreference extends DialogPreference {


    @Bind(R.id.minute_text) TextView timeView;
    @Bind(R.id.minute) NumberPicker numberPicker;
    @Inject PrefsManager prefs;


    public SleepDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        App.getComponent().inject(this);
    }

    @Override
    protected void showDialog(Bundle state) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sleep_timer, null);
        ButterKnife.bind(this, view);

        //init views
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
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