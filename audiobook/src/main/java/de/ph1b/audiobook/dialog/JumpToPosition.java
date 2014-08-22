package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.service.AudioPlayerService;


public class JumpToPosition extends DialogFragment {

    private int duration;
    private int biggestHour;
    private NumberPicker mPicker;
    private NumberPicker hPicker;

    public static final String POSITION = "position";
    public static final String DURATION = "duration";

    private int maxMinuteOfHour(int hour) {
        int hourInMinutes = hour * 60;
        int durationInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration);

        if (hour == biggestHour) {
            return (durationInMinutes - hourInMinutes) % 60;
        } else {
            return 59;
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.time_picker, null);

        Bundle bundle = this.getArguments();
        duration = bundle.getInt(DURATION, 0);
        int position = bundle.getInt(POSITION, 0);


        hPicker = (NumberPicker) v.findViewById(R.id.number_hour);
        mPicker = (NumberPicker) v.findViewById(R.id.number_minute);

        //set maximum values
        biggestHour = (int) TimeUnit.MILLISECONDS.toHours(duration);
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
                mPicker.setMaxValue(maxMinuteOfHour(newVal));
            }
        });

        mPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                int hValue = hPicker.getValue();

                //scrolling forward
                if (oldVal == 59 && newVal == 0){
                    hPicker.setValue(++hValue);

                //scrolling backward
                } if (oldVal == 0 && newVal ==59){
                    hPicker.setValue(--hValue);
                }
            }
        });

        builder.setView(v);
        builder.setTitle(R.string.action_jump_to);

        builder.setPositiveButton(R.string.choose_time_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int h = hPicker.getValue();
                int m = mPicker.getValue();
                int newPosition = (m+60*h)*60*1000;
                Intent i = new Intent(AudioPlayerService.CONTROL_CHANGE_MEDIA_POSITION);
                i.putExtra(AudioPlayerService.CONTROL_CHANGE_MEDIA_POSITION, newPosition);
                Context c = getActivity().getApplication();
                LocalBroadcastManager.getInstance(c).sendBroadcast(i);

            }
        });
        builder.setNegativeButton(R.string.choose_time_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }


}
