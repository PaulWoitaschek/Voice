package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.BaseApplication;

public class JumpToPositionDialog extends DialogFragment {

    private static final String TAG = JumpToPositionDialog.class.getSimpleName();
    private int durationInMinutes;
    private int biggestHour;
    private NumberPicker mPicker;
    private NumberPicker hPicker;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_time_picker, null);

        hPicker = (NumberPicker) v.findViewById(R.id.number_hour);
        mPicker = (NumberPicker) v.findViewById(R.id.number_minute);
        BaseApplication baseApplication = (BaseApplication) getActivity().getApplication();

        final Book book = baseApplication.getCurrentBook();
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        int duration = book.getCurrentChapter().getDuration();
        int position = book.getTime();
        biggestHour = (int) TimeUnit.MILLISECONDS.toHours(duration);
        durationInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration);
        if (biggestHour == 0) { //sets visibility of hour related things to gone if max.hour is zero
            v.findViewById(R.id.colon).setVisibility(View.GONE);
            hPicker.setVisibility(View.GONE);
        }

        //set maximum values
        hPicker.setMaxValue(biggestHour);
        if (biggestHour == 0) {
            mPicker.setMaxValue((int) TimeUnit.MILLISECONDS.toMinutes(duration));
        } else {
            mPicker.setMaxValue(59);
        }

        //set default values
        int defaultHour = (int) TimeUnit.MILLISECONDS.toHours(position);
        int defaultMinute = (int) TimeUnit.MILLISECONDS.toMinutes(position) % 60;
        hPicker.setValue(defaultHour);
        mPicker.setValue(defaultMinute);

        hPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (newVal == biggestHour) {
                    mPicker.setMaxValue((durationInMinutes - newVal * 60) % 60);
                } else {
                    mPicker.setMaxValue(59);
                }
            }
        });

        mPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int hValue = hPicker.getValue();

                if (oldVal == 59 && newVal == 0) //scrolling forward
                {
                    hPicker.setValue(++hValue);
                }
                if (oldVal == 0 && newVal == 59) //scrolling backward
                {
                    hPicker.setValue(--hValue);
                }
            }
        });

        ThemeUtil.theme(mPicker);
        ThemeUtil.theme(hPicker);

        return new MaterialDialog.Builder(getActivity())
                .customView(v, true)
                .title(R.string.action_time_change)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        int h = hPicker.getValue();
                        int m = mPicker.getValue();
                        int newPosition = (m + 60 * h) * 60 * 1000;
                        new ServiceController(getActivity()).changeTime(newPosition, book.getCurrentChapter().getPath());
                    }
                })
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .build();
    }
}
