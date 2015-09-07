package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.text.DecimalFormat;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.ThemeUtil;

/**
 * Dialog for setting the playback speed of the current book.
 */
public class PlaybackSpeedDialogFragment extends DialogFragment {

    public static final String TAG = PlaybackSpeedDialogFragment.class.getSimpleName();
    private static final float SPEED_DELTA = 0.02f;
    private static final float SPEED_MIN = 0.5f;
    private static final float SPEED_MAX = 2f;
    private static final int MAX_STEPS = Math.round((SPEED_MAX - SPEED_MIN) / SPEED_DELTA);

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static float speedStepValueToSpeed(int step) {
        return (SPEED_MIN + (step * SPEED_DELTA));
    }

    private static int speedValueToSteps(float speed) {
        return Math.round((speed - SPEED_MIN) * (MAX_STEPS + 1) / (SPEED_MAX - SPEED_MIN));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // init views
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_amount_chooser, null);
        SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView textView = (TextView) v.findViewById(R.id.textView);
        ThemeUtil.theme(seekBar);

        // setting current speed
        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final Book book = db.getBook(PrefsManager.getInstance(getActivity()).getCurrentBookId());
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        float speed = book.getPlaybackSpeed();
        textView.setText(formatTime(speed));
        seekBar.setMax(MAX_STEPS);
        seekBar.setProgress(speedValueToSteps(speed));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private final ServiceController serviceController = new ServiceController(getActivity());

            @Override
            public void onProgressChanged(SeekBar seekBar, int step, boolean fromUser) {
                float newSpeed = speedStepValueToSpeed(step);
                textView.setText(formatTime(newSpeed));
                serviceController.setPlaybackSpeed(newSpeed);
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
                .customView(v, true)
                .build();
    }

    private String formatTime(float time) {
        return getString(R.string.playback_speed) + ": " + df.format(time) + "x";
    }
}
