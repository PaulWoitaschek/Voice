package de.ph1b.audiobook.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Display;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.NoExternalStorage;


public class CommonTasks {


    public static Bitmap genCapital(String bookName, int pxSize, Resources resources) {
        Bitmap thumb = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(thumb);
        Paint textPaint = new Paint();
        textPaint.setTextSize(2 * pxSize / 3);
        textPaint.setColor(resources.getColor(android.R.color.white));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(resources.getColor(R.color.file_chooser_audio));
        canvas.drawRect(0, 0, pxSize, pxSize, backgroundPaint);
        int y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
        canvas.drawText(bookName.substring(0, 1).toUpperCase(), pxSize / 2, y, textPaint);
        return thumb;
    }

    public static int convertDpToPx(float dp, Resources r) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    public static int getDisplayMinSize(Activity a) {
        Display display = a.getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        return width < height ? width : height;
    }

    public static void checkExternalStorage(Context c) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Intent i = new Intent(c, NoExternalStorage.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(new Intent(i));
        }
    }
}
