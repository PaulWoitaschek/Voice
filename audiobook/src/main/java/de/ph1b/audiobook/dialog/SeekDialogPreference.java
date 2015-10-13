package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.utils.App;

public class SeekDialogPreference extends DialogPreference {

    private static final int SEEK_BAR_MIN = 10;
    private static final int SEEK_BAR_MAX = 60;
    @Bind(R.id.seekBar) SeekBar seekBar;
    @Bind(R.id.textView) TextView textView;
    @Inject PrefsManager prefs;

    public SeekDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        App.getComponent().inject(this);
    }

    @Override
    protected void showDialog(Bundle state) {
        @SuppressLint("InflateParams") View customView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_amount_chooser, null);
        ButterKnife.bind(this, customView);

        //seekBar
        int position = prefs.getSeekTime();

        MDTintHelper.setTint(seekBar, ContextCompat.getColor(getContext(), R.color.accent));
        seekBar.setMax(SEEK_BAR_MAX - SEEK_BAR_MIN);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + SEEK_BAR_MIN;
                textView.setText(getContext().getResources().getQuantityString(R.plurals.seconds, value, value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setProgress(position - SEEK_BAR_MIN);

        new MaterialDialog.Builder(getContext())
                .title(R.string.pref_seek_time)
                .customView(customView, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        int seekAmount = seekBar.getProgress();
                        prefs.setSeekTime(seekAmount + SEEK_BAR_MIN);
                    }
                })
                .show();
    }
}
