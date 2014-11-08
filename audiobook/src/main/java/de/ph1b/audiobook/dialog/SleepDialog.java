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
import android.widget.TextView;

import de.ph1b.audiobook.R;


public class SleepDialog extends DialogFragment {

    public static final String TAG = "de.ph1b.audiobook.dialog.SleepDialog";

    private TextView timeView;
    private NumberPicker mPicker;
    private SleepTimeCallback sleepTimeCallback;

    public interface SleepTimeCallback {
        public void onSleepTimeChosen(int sleepTime);
    }


    @NonNull
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
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                SleepTimeCallback fragment = (SleepTimeCallback) getTargetFragment();
                fragment.onSleepTimeChosen(mPicker.getValue() * 60 * 1000);
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.setTitle(R.string.action_sleep_title);

        return builder.create();
    }
}