package de.ph1b.audiobook.utils;

import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Build;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;

import java.lang.reflect.Field;

import de.ph1b.audiobook.R;

public class ThemeUtil {

    public static int getCurrentTheme(){
        return 0;
    }

    public static void theme(SeekBar seekBar) {
        if (Build.VERSION.SDK_INT < 21) {
            Resources resources = seekBar.getResources();

            seekBar.getProgressDrawable().setColorFilter(resources.getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            if (Build.VERSION.SDK_INT >= 16) {
                seekBar.getThumb().setColorFilter(resources.getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }


    public static void theme(NumberPicker numberPicker) {
        if (Build.VERSION.SDK_INT < 21) {
            if (numberPicker != null) {
                Resources r = numberPicker.getResources();
                final int count = numberPicker.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = numberPicker.getChildAt(i);
                    if (child instanceof EditText) {
                        try {
                            Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
                            selectorWheelPaintField.setAccessible(true);
                            ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(r.getColor(R.color.colorAccent));
                            ((EditText) child).setTextColor(r.getColor(R.color.colorAccent));
                            numberPicker.invalidate();
                        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
