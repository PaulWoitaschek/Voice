package de.paul_woitaschek.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.text.DecimalFormat;

import de.paul_woitaschek.audiobook.R;
import de.paul_woitaschek.audiobook.model.Book;
import de.paul_woitaschek.audiobook.service.ServiceController;
import de.paul_woitaschek.audiobook.uitools.ThemeUtil;
import de.paul_woitaschek.audiobook.utils.BaseApplication;

public class SetPlaybackSpeedDialog extends DialogFragment {

    private static final String TAG = SetPlaybackSpeedDialog.class.getSimpleName();
    private static final float SPEED_DELTA = 0.1f;
    private static final float SPEED_MIN = 0.5f;
    private static final float SPEED_MAX = 2f;
    private float speed;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_amount_chooser, null);

        SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView textView = (TextView) v.findViewById(R.id.textView);

        // setting current speed
        BaseApplication baseApplication = (BaseApplication) getActivity().getApplication();
        Book book = baseApplication.getCurrentBook();
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        speed = book.getPlaybackSpeed();
        textView.setText(formatTime(speed));

        int seekMaxSteps = (int) ((SPEED_MAX - SPEED_MIN) / SPEED_DELTA);
        seekBar.setMax(seekMaxSteps);
        int seekProgress = (int) ((speed - SPEED_MIN) * (seekMaxSteps + 1) / (SPEED_MAX - SPEED_MIN));
        seekBar.setProgress(seekProgress);

        seekBar.getProgressDrawable().setColorFilter(ThemeUtil.getColorAccent(getActivity()), PorterDuff.Mode.SRC_ATOP);
        if (Build.VERSION.SDK_INT >= 16) {
            seekBar.getThumb().setColorFilter(ThemeUtil.getColorAccent(getActivity()), PorterDuff.Mode.SRC_ATOP);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int step, boolean fromUser) {
                speed = speedStepValueToSpeed(step);
                textView.setText(formatTime(speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.playback_speed)
                .negativeText(R.string.dialog_cancel)
                .positiveText(R.string.dialog_confirm)
                .customView(v, true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        new ServiceController(getActivity()).setPlaybackSpeed(speed);
                    }
                })
                .build();
    }

    private String formatTime(float time) {
        DecimalFormat df = new DecimalFormat("0.00");
        return getString(R.string.playback_speed) + ": " + df.format(time) + "x";
    }

    private float speedStepValueToSpeed(int step) {
        return (SPEED_MIN + (step * SPEED_DELTA));
    }
}
