package de.ph1b.audiobook.dialog.prefs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;

import java.text.DecimalFormat;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.interfaces.SettingsSetListener;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.utils.App;

/**
 * Dialog for setting the playback speed of the current book.
 *
 * @author Paul Woitaschek
 */
public class PlaybackSpeedDialogFragment extends DialogFragment {

    public static final String TAG = PlaybackSpeedDialogFragment.class.getSimpleName();
    private static final float SPEED_DELTA = 0.02f;
    private static final float SPEED_MIN = 0.5f;
    private static final float SPEED_MAX = 2f;
    private static final int MAX_STEPS = Math.round((SPEED_MAX - SPEED_MIN) / SPEED_DELTA);

    private static final DecimalFormat df = new DecimalFormat("0.00");
    @Bind(R.id.seekBar) SeekBar seekBar;
    @Bind(R.id.textView) TextView textView;
    @Inject PrefsManager prefs;
    @Inject DataBaseHelper db;
    @Nullable private SettingsSetListener listener;

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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_amount_chooser, null);
        ButterKnife.bind(this, v);
        App.getComponent().inject(this);

        MDTintHelper.setTint(seekBar, ContextCompat.getColor(getContext(), R.color.accent));

        // setting current speed
        final Book book = db.getBook(prefs.getCurrentBookId());
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        float speed = book.playbackSpeed();
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

    public void setSettingsSetListener(@Nullable SettingsSetListener listener) {
        this.listener = listener;
    }
}
