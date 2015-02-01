package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.utils.MaterialCompatThemer;


public class JumpToPosition extends DialogFragment {

    public static final String POSITION = "position";
    public static final String DURATION = "duration";
    private int durationInMinutes;
    private int biggestHour;
    private NumberPicker mPicker;
    private NumberPicker hPicker;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_time_picker, null);

        hPicker = (NumberPicker) v.findViewById(R.id.number_hour);
        mPicker = (NumberPicker) v.findViewById(R.id.number_minute);

        Bundle bundle = this.getArguments();
        int duration = bundle.getInt(DURATION, 0);
        int position = bundle.getInt(POSITION, 0);
        biggestHour = (int) TimeUnit.MILLISECONDS.toHours(duration);
        durationInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration);
        if (biggestHour == 0) { //sets visibility of hour related things to gone if max.hour is zero
            v.findViewById(R.id.hours).setVisibility(View.GONE);
            v.findViewById(R.id.colon).setVisibility(View.GONE);
            hPicker.setVisibility(View.GONE);
        }

        //set maximum values
        hPicker.setMaxValue(biggestHour);
        if (biggestHour == 0)
            mPicker.setMaxValue((int) TimeUnit.MILLISECONDS.toMinutes(duration));
        else
            mPicker.setMaxValue(59);

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
                    hPicker.setValue(++hValue);
                if (oldVal == 0 && newVal == 59) //scrolling backward
                    hPicker.setValue(--hValue);
            }
        });

        builder.setView(v);
        builder.setTitle(R.string.action_jump_to);

        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int h = hPicker.getValue();
                int m = mPicker.getValue();
                int newPosition = (m + 60 * h) * 60 * 1000;
                new ServiceController(getActivity()).changeTime(newPosition);
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialCompatThemer.theme(getDialog());
    }
}
