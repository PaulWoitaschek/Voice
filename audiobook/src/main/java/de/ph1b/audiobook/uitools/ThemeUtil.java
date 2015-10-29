package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AnyRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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
            int colorAccent = ContextCompat.getColor(numberPicker.getContext(), R.color.accent);
            for (int i = 0, count = numberPicker.getChildCount(); i < count; i++) {
                View child = numberPicker.getChildAt(i);
                try {
                    Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(colorAccent);
                    ((EditText) child).setTextColor(colorAccent);
                    numberPicker.invalidate();
                } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
                try {
                    Field f1 = Class.forName("android.widget.NumberPicker").getDeclaredField("mSelectionDivider");
                    f1.setAccessible(true);
                    Drawable dividerDrawable = DrawableCompat.wrap((Drawable) f1.get(numberPicker));
                    DrawableCompat.setTint(dividerDrawable, colorAccent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            numberPicker.invalidate();
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
