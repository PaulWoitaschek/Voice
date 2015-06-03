package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.PorterDuff;
import android.os.Build;
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
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.PrefsManager;

public class PlaybackSpeedDialogFragment extends DialogFragment {

    public static final String TAG = PlaybackSpeedDialogFragment.class.getSimpleName();
    private static final float SPEED_DELTA = 0.1f;
    private static final float SPEED_MIN = 0.5f;
    private static final float SPEED_MAX = 2f;
    private float speed;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_amount_chooser, null);

        SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        final TextView textView = (TextView) v.findViewById(R.id.textView);

        // setting current speed
        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final Book book = db.getBook(PrefsManager.getInstance(getActivity()).getCurrentBookId());
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        speed = book.getPlaybackSpeed();
        textView.setText(formatTime(speed));

        int seekMaxSteps = (int) ((SPEED_MAX - SPEED_MIN) / SPEED_DELTA);
        seekBar.setMax(seekMaxSteps);
        int seekProgress = (int) ((speed - SPEED_MIN) * (seekMaxSteps + 1) / (SPEED_MAX - SPEED_MIN));
        seekBar.setProgress(seekProgress);

        int color = getResources().getColor(ThemeUtil.getResourceId(getActivity(), R.attr.colorAccent));
        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
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
                        Book currentBook = db.getBook(book.getId());
                        if (currentBook != null) {
                            currentBook.setPlaybackSpeed(speed);
                            db.updateBook(currentBook);
                        }
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
