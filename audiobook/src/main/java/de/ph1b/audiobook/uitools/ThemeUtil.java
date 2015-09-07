package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.AnyRes;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

import de.ph1b.audiobook.R;

public class ThemeUtil {

    public static int getTheme(@NonNull Context c) {
        Resources r = c.getResources();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String theme = sp.getString(r.getString(R.string.pref_key_theme), null);
        switch (theme == null ? "light" : theme) {
            case "light":
                return R.style.LightTheme;
            case "dark":
                return R.style.DarkTheme;
            default:
                throw new AssertionError("Unknown theme found=" + theme);
        }
    }

    @AnyRes
    public static int getResourceId(@NonNull Context c, @AttrRes int attr) {
        TypedArray ta = c.obtainStyledAttributes(new int[]{attr});
        int resId = ta.getResourceId(0, -1);
        ta.recycle();
        if (resId == -1) {
            throw new IllegalArgumentException("Resource with attr=" + attr + " not found");
        }
        return resId;
    }

    public static void theme(@NonNull NumberPicker numberPicker) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final int count = numberPicker.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = numberPicker.getChildAt(i);
                if (child instanceof EditText) {
                    try {
                        Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
                        selectorWheelPaintField.setAccessible(true);
                        @SuppressWarnings("deprecation") int colorAccent = numberPicker.getResources().getColor(ThemeUtil.getResourceId(numberPicker.getContext(), R.attr.colorAccent));
                        ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(colorAccent);
                        ((EditText) child).setTextColor(colorAccent);
                        numberPicker.invalidate();
                    } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
