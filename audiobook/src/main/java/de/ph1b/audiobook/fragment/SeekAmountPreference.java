package de.ph1b.audiobook.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;


public class SeekAmountPreference extends DialogPreference {

    private final int SEEK_BAR_MIN = 10;
    private SeekBar seekBar;
    private static final String TAG = "de.ph1b.audiobook.fragment.SeekAmountPreference";

    public SeekAmountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(context.getString(R.string.change_forward_amount));
        setDialogLayoutResource(R.layout.change_forward_amount_dialog);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        seekBar = (SeekBar) view.findViewById(R.id.change_forward_amount);
        final TextView textView = (TextView) view.findViewById(R.id.change_forward_text_amount);
        seekBar.setMax(60 - SEEK_BAR_MIN);
        SharedPreferences sp = getSharedPreferences();

        int position = sp.getInt(getContext().getString(R.string.pref_change_amount), 20);
        seekBar.setProgress(position - SEEK_BAR_MIN);


        textView.setText(String.valueOf(seekBar.getProgress() + SEEK_BAR_MIN) + " " + getContext().getString(R.string.seconds)  );

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(String.valueOf(progress + SEEK_BAR_MIN) + " " + getContext().getString(R.string.seconds));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int seekAmount = seekBar.getProgress();
            if (seekAmount > 0) {
                SharedPreferences.Editor editor = getEditor();
                editor.putInt(getContext().getString(R.string.pref_change_amount), seekAmount + SEEK_BAR_MIN);
                editor.apply();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Put new seek-amount to shared Preferences: " + (seekAmount + SEEK_BAR_MIN));
            }
        }
    }
}
