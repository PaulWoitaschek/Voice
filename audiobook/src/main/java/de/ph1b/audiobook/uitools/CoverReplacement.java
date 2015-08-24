package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;

import com.google.common.base.Preconditions;

import de.ph1b.audiobook.R;


public class CoverReplacement extends Drawable {

    private final String text;
    private final Paint textPaint;
    private final int backgroundColor;

    public CoverReplacement(@NonNull String text, @NonNull Context c) {
        Preconditions.checkArgument(!text.isEmpty());
        this.text = text;

        // text
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Align.CENTER);

        // background
        boolean lightTheme = ThemeUtil.getTheme(c) == R.style.LightTheme;
        //noinspection deprecation
        backgroundColor = c.getResources().getColor(lightTheme ? R.color.light_primary_dark : R.color.dark_primary_dark);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = getBounds();
        int height = rect.height();
        int width = rect.width();

        textPaint.setTextSize(2f * width / 3f);

        canvas.drawColor(backgroundColor);
        float y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(text, 0, 1, width / 2f, y, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return textPaint.getAlpha();
    }
}
