package de.ph1b.audiobook.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import de.ph1b.audiobook.R;


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

        timeView = (TextView) view.findViewById(R.id.minute_text);
        numberPicker = (NumberPicker) view.findViewById(R.id.minute);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(20);
        timeView.setText(" " + String.valueOf(numberPicker.getValue()) + " ");
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                timeView.setText(" " + String.valueOf(newVal) + " ");
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int sleepAmount = numberPicker.getValue();
            SharedPreferences.Editor editor = getEditor();
            editor.putInt(getContext().getString(R.string.pref_key_sleep_time), sleepAmount);
            editor.apply();
        }
    }
}