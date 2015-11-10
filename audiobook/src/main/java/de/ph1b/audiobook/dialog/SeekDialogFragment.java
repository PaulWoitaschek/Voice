package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.SettingsSetListener;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.utils.App;

public class SeekDialogFragment extends DialogFragment {

    public static final String TAG = SeekDialogFragment.class.getSimpleName();
    private static final int SEEK_BAR_MIN = 10;
    private static final int SEEK_BAR_MAX = 60;
    @Bind(R.id.seekBar) SeekBar seekBar;
    @Bind(R.id.textView) TextView textView;
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
        @SuppressLint("InflateParams") View customView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_amount_chooser, null);
        ButterKnife.bind(this, customView);

        //seekBar
        final int oldSeekTime = prefs.getSeekTime();

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
        seekBar.setProgress(oldSeekTime - SEEK_BAR_MIN);

        return new MaterialDialog.Builder(getContext())
                .title(R.string.pref_seek_time)
                .customView(customView, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive((materialDialog, dialogAction) -> {
                    int newSeekTime = seekBar.getProgress() + SEEK_BAR_MIN;
                    prefs.setSeekTime(newSeekTime);
                    settingsSetListener.onSettingsSet(oldSeekTime != newSeekTime);
                })
                .build();
    }
}
