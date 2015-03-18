package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.SeekBar;

import java.lang.reflect.Field;

import de.ph1b.audiobook.R;

public class ThemeUtil {

    public static void theme(SeekBar seekBar) {
        if (Build.VERSION.SDK_INT < 21) {
            int colorAccent = getColorAccent(seekBar.getContext());
            seekBar.getProgressDrawable().setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
            if (Build.VERSION.SDK_INT >= 16) {
                seekBar.getThumb().setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    public static int getTextColorPrimary(Context c) {
        int[] attrs = new int[]{R.attr.colorPrimary};
        TypedArray ta = c.obtainStyledAttributes(attrs);
        try {
            return ta.getColor(0, Color.BLACK);
        } finally {
            ta.recycle();
        }
    }

    public static int getColorAccent(Context c) {
        int[] attrs = new int[]{R.attr.colorAccent};
        TypedArray ta = c.obtainStyledAttributes(attrs);
        try {
            return ta.getColor(0, Color.BLACK);
        } finally {
            ta.recycle();
        }
    }


    public static int getTheme(Context c) {
        Resources r = c.getResources();
        String themeLight = r.getString(R.string.pref_theme_light);
        String themeDark = r.getString(R.string.pref_theme_dark);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String theme = sp.getString(r.getString(R.string.pref_key_theme), themeLight);

        if (theme.equals(themeLight)) {
            return R.style.LightTheme;
        } else if (theme.equals(themeDark)) {
            return R.style.DarkTheme;
        } else {
            throw new IllegalArgumentException("Unknown theme found=" + theme);
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
                            int colorAccent = getColorAccent(numberPicker.getContext());
                            ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(colorAccent);
                            ((EditText) child).setTextColor(r.getColor(colorAccent));
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
