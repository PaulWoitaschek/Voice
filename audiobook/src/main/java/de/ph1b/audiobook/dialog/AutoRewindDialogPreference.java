package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.PrefsManager;


public class AutoRewindDialogPreference extends DialogPreference {

    private static final int SEEK_BAR_MIN = 0;
    private static final int SEEK_BAR_MAX = 20;
    private TextView textView;

    public AutoRewindDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void setText(int progress) {
        String autoRewindSummary = getContext().getString(R.string.pref_auto_rewind_summary)
                + " " + String.valueOf(progress + SEEK_BAR_MIN) + " "
                + getContext().getString(R.string.seconds);
        textView.setText(autoRewindSummary);
    }

    @Override
    protected void showDialog(Bundle state) {
        final PrefsManager prefs = new PrefsManager(getContext());

        // init custom view
        @SuppressLint("InflateParams") View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_amount_chooser, null);
        final SeekBar seekBar = (SeekBar) customView.findViewById(R.id.seekBar);
        textView = (TextView) customView.findViewById(R.id.textView);

        //seekBar
        ThemeUtil.theme(seekBar);
        int position = prefs.getAutoRewindAmount();
        seekBar.setMax(SEEK_BAR_MAX - SEEK_BAR_MIN);
        seekBar.setProgress(position - SEEK_BAR_MIN);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // text
        setText(seekBar.getProgress());

        new MaterialDialog.Builder(getContext())
                .title(R.string.pref_auto_rewind_title)
                .customView(customView, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        int autoRewindAmount = seekBar.getProgress();
                        prefs.setAutoRewindAmount(autoRewindAmount + SEEK_BAR_MIN);
                    }
                })
                .show();
    }
}
