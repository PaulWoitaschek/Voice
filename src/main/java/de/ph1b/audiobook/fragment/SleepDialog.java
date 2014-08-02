package de.ph1b.audiobook.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.service.AudioPlayerService;


public class SleepDialog extends DialogFragment {

    private TextView timeView;
    private NumberPicker mPicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //suppress because dialog!
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_sleep_timer, null);

        timeView = (TextView) v.findViewById(R.id.minute_text);
        mPicker = (NumberPicker) v.findViewById(R.id.minute);
        mPicker.setMinValue(1);
        mPicker.setMaxValue(60);
        mPicker.setValue(20);
        timeView.setText(" " + String.valueOf(mPicker.getValue()) + " ");
        mPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                timeView.setText(" " + String.valueOf(newVal) + " ");
            }
        });

        builder.setView(v);
        builder.setPositiveButton(R.string.choose_time_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Context c = getActivity().getApplicationContext();
                Intent i = new Intent(AudioPlayerService.CONTROL_SLEEP);
                i.putExtra(AudioPlayerService.CONTROL_SLEEP, mPicker.getValue() * 60 * 1000);
                LocalBroadcastManager.getInstance(c).sendBroadcast(i);
            }
        });
        builder.setNegativeButton(R.string.choose_time_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setTitle(R.string.action_sleep_title);

        return builder.create();
    }
}