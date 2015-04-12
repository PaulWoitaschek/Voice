package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.Validate;


public class CoverReplacement extends Drawable {

    private final String text;
    private final Paint textPaint;
    private final Paint backgroundPaint;

    public CoverReplacement(String text, Context c) {
        Validate.notNull(text);
        Validate.notEmpty(text);
        this.text = text;

        // text
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Align.CENTER);

        // background
        int color;
        if (ThemeUtil.getTheme(c) == R.style.LightTheme) {
            color = c.getResources().getColor(R.color.light_primary_dark);
        } else {
            color = c.getResources().getColor(R.color.dark_primary_dark);
        }
        backgroundPaint = new Paint();
        backgroundPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = getBounds();
        int height = rect.height();
        int width = rect.width();

        textPaint.setTextSize(2f * width / 3f);

        canvas.drawRect(0, 0, width, height, backgroundPaint);
        float y = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(text, 0, 1, width / 2f, y, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
        backgroundPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
        backgroundPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return textPaint.getAlpha();
    }
}
