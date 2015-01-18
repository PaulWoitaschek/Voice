package de.ph1b.audiobook.dialog;

import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.MaterialCompatThemer;
import de.ph1b.audiobook.utils.Prefs;


public class SeekPreferenceDialog extends DialogPreference {

    private final int SEEK_BAR_MIN = 10;
    private SeekBar seekBar;
    private final Prefs prefs;

    public SeekPreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = new Prefs(context);
        setDialogTitle(context.getString(R.string.pref_seek_time));
        setDialogLayoutResource(R.layout.dialog_amount_chooser);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        MaterialCompatThemer.theme(getDialog());
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        final TextView textView = (TextView) view.findViewById(R.id.textView);
        seekBar.setMax(60 - SEEK_BAR_MIN);

        int position = prefs.getSeekTime();
        seekBar.setProgress(position - SEEK_BAR_MIN);

        textView.setText(String.valueOf(seekBar.getProgress() + SEEK_BAR_MIN) + " " + getContext().getString(R.string.seconds));

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


        MaterialCompatThemer.theme(seekBar);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int seekAmount = seekBar.getProgress();
            prefs.setSeekTime(seekAmount + SEEK_BAR_MIN);
        }
    }
}
