package de.ph1b.audiobook.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;

import de.ph1b.audiobook.R;


public class SleepPreferenceDialog extends DialogPreference {

    private TextView timeView;
    private NumberPicker numberPicker;
    private CheckBox checkBox;

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
        checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        //init checkbox
        final String trackToEndPrefKey = getContext().getString(R.string.pref_key_play_track_to_end);
        boolean playTrackToEnd = getSharedPreferences().getBoolean(trackToEndPrefKey, false);
        checkBox.setChecked(playTrackToEnd);

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
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SharedPreferences.Editor editor = getEditor();

            int sleepAmount = numberPicker.getValue();
            boolean playTrackToEnd = checkBox.isChecked();

            editor.putInt(getContext().getString(R.string.pref_key_sleep_time), sleepAmount);
            editor.putBoolean(getContext().getString(R.string.pref_key_play_track_to_end), playTrackToEnd);

            editor.apply();
        }
    }
}