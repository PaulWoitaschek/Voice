package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;

import de.ph1b.audiobook.R;

public class SetPlaybackSpeedDialog extends DialogFragment {

    public interface PlaybackSpeedChanged {
        public void onSpeedChanged(float speed);
    }

    public static final String DEFAULT_AMOUNT = "defaultAmount";
    private static final float speedDelta = 0.1f;
    private static final float minSpeed = 0.5f;
    private static final float maxSpeed = 2f;
    private float speed;

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_amount_chooser, null);

        SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView textView = (TextView) v.findViewById(R.id.textView);

        Bundle bundle = getArguments();
        speed = bundle.getFloat(DEFAULT_AMOUNT, 1);
        textView.setText(formatTime(speed));

        int seekMaxSteps = (int) ((maxSpeed - minSpeed) / speedDelta);
        seekBar.setMax(seekMaxSteps);
        int seekProgress = (int) ((speed - minSpeed) * (seekMaxSteps + 1) / (maxSpeed - minSpeed));
        seekBar.setProgress(seekProgress);

        builder.setTitle(getString(R.string.playback_speed));
        builder.setView(v);

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

        builder.setPositiveButton(getString(R.string.dialog_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((PlaybackSpeedChanged) getTargetFragment()).onSpeedChanged(speed);
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_cancel), null);

        return builder.create();
    }

    private float speedStepValueToSpeed(int step) {
        return (minSpeed + (step * speedDelta));
    }

    private String formatTime(float time) {
        DecimalFormat df = new DecimalFormat("0.00");
        return getString(R.string.playback_speed) + ": " + df.format(time) + "x";
    }
}
