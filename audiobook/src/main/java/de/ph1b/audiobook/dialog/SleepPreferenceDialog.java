package de.ph1b.audiobook.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.MaterialCompatThemer;


public class SleepPreferenceDialog extends DialogPreference {

    private TextView timeView;
    private NumberPicker numberPicker;

    public SleepPreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(context.getResources().getString(R.string.pref_sleep_time));
        setDialogLayoutResource(R.layout.dialog_sleep_timer);
    }


    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        //init views
        timeView = (TextView) view.findViewById(R.id.minute_text);
        numberPicker = (NumberPicker) view.findViewById(R.id.minute);

        //init number picker
        int currentSleepValue = getSharedPreferences().getInt(getContext().getString(R.string.pref_key_sleep_time), 20);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(currentSleepValue);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                timeView.setText(" " + String.valueOf(newVal) + " ");
            }
        });

        //init text
        timeView.setText(" " + String.valueOf(numberPicker.getValue()) + " ");

        MaterialCompatThemer.theme(numberPicker);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        MaterialCompatThemer.theme(getDialog());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences.Editor editor = getEditor();

            int sleepAmount = numberPicker.getValue();
            editor.putInt(getContext().getString(R.string.pref_key_sleep_time), sleepAmount);
            editor.apply();
        }
    }
}