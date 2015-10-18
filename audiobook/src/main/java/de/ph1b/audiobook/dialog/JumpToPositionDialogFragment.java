package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.persistence.BookShelf;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.App;

public class JumpToPositionDialogFragment extends DialogFragment {

    public static final String TAG = JumpToPositionDialogFragment.class.getSimpleName();
    @Bind(R.id.number_minute) NumberPicker mPicker;
    @Bind(R.id.number_hour) NumberPicker hPicker;
    @Bind(R.id.colon) View colon;
    @Inject PrefsManager prefs;
    @Inject BookShelf db;
    private int durationInMinutes;
    private int biggestHour;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_time_picker, null);
        ButterKnife.bind(this, v);
        App.getComponent().inject(this);


        final Book book = db.getBook(prefs.getCurrentBookId());
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }
        int duration = book.currentChapter().duration();
        int position = book.time();
        biggestHour = (int) TimeUnit.MILLISECONDS.toHours(duration);
        durationInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(duration);
        if (biggestHour == 0) { //sets visibility of hour related things to gone if max.hour is zero
            colon.setVisibility(View.GONE);
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        int h = hPicker.getValue();
                        int m = mPicker.getValue();
                        int newPosition = (m + 60 * h) * 60 * 1000;
                        new ServiceController(getActivity()).changeTime(newPosition, book.currentChapter().file());
                    }
                })
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .build();
    }
}
