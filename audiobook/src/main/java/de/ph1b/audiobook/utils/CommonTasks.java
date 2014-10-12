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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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


    public static String[] saveCovers(Bitmap cover, Activity a) {
        String thumbPath = "";
        String coverPath = "";

        String packageName = a.getPackageName();
        String fileName = String.valueOf(System.currentTimeMillis()) + ".png";
        int pixelCut = 10;
        // if cover is too big, scale it down
        int displayPx = CommonTasks.getDisplayMinSize(a);
        if (cover.getWidth() > displayPx || cover.getHeight() > displayPx) {
            cover = Bitmap.createScaledBitmap(cover, displayPx, displayPx, false);
        }
        cover = Bitmap.createBitmap(cover, pixelCut, pixelCut, cover.getWidth() - 2 * pixelCut, cover.getHeight() - 2 * pixelCut); //crop n px from each side for poor images
        int thumbPx = CommonTasks.convertDpToPx(a.getResources().getDimension(R.dimen.thumb_size), a.getResources());
        Bitmap thumb = Bitmap.createScaledBitmap(cover, thumbPx, thumbPx, false);
        File thumbDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/thumbs");
        File imageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/images");
        File thumbFile = new File(thumbDir, fileName);
        File imageFile = new File(imageDir, fileName);
        //noinspection ResultOfMethodCallIgnored
        thumbDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        imageDir.mkdirs();
        try {
            FileOutputStream coverOut = new FileOutputStream(imageFile);
            FileOutputStream thumbOut = new FileOutputStream(thumbFile);
            cover.compress(Bitmap.CompressFormat.PNG, 90, coverOut);
            thumb.compress(Bitmap.CompressFormat.PNG, 90, thumbOut);
            coverOut.flush();
            thumbOut.flush();
            coverOut.close();
            thumbOut.close();
            coverPath = imageFile.getAbsolutePath();
            thumbPath = thumbFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!coverPath.equals("") && !thumbPath.equals(""))
            return new String[]{coverPath, thumbPath};
        else
            return null;
    }
}
