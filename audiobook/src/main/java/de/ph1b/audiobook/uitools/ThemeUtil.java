package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.AnyRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

import de.ph1b.audiobook.R;

public class ThemeUtil {

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

    public enum Theme {
        LIGHT(R.style.LightTheme, R.string.pref_theme_light, R.color.light_primary_dark),
        DARK(R.style.DarkTheme, R.string.pref_theme_dark, R.color.dark_primary_dark);

        @StyleRes private final int themeId;
        @StringRes private final int nameId;
        @ColorRes private final int colorId;

        Theme(@StyleRes int themeId, @StringRes int nameId, @ColorRes int colorId) {
            this.themeId = themeId;
            this.nameId = nameId;
            this.colorId = colorId;
        }

        @StyleRes
        public int getThemeId() {
            return themeId;
        }

        @ColorRes
        public int getColorId() {
            return colorId;
        }

        @StringRes
        public int getNameId() {
            return nameId;
        }
    }
}
