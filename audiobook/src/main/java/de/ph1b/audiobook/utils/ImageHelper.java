package de.ph1b.audiobook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.ph1b.audiobook.R;


public class ImageHelper {

    private static final String TAG = ImageHelper.class.getSimpleName();

    public static Bitmap genCapital(String bookName, Context c) {
        int reqLength = getCoverLength(c);
        Bitmap bitmap = Bitmap.createBitmap(reqLength, reqLength, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint textPaint = new Paint();
        textPaint.setTextSize(2 * reqLength / 3);
        textPaint.setColor(c.getResources().getColor(android.R.color.white));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(c.getResources().getColor(R.color.colorAccent));
        canvas.drawRect(0, 0, reqLength, reqLength, backgroundPaint);
        int y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
        canvas.drawText(bookName.substring(0, 1).toUpperCase(), reqLength / 2, y, textPaint);
        return bitmap;
    }


    private static int getCoverLength(Context c) {
        c.getResources().getDisplayMetrics();
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;
        return displayWidth < displayHeight ? displayWidth : displayHeight;
    }

    /**
     * Saves a bitmap as a jpg file to the personal directory.
     *
     * @param bitmap The bitmap to be saved
     * @param c      Application context
     * @return the path where the bitmap has been saved.
     */
    public static String saveCover(Bitmap bitmap, Context c) {
        String coverPath;

        int width = bitmap.getWidth();
        int heigth = bitmap.getHeight();
        int relation = width / heigth;
        if (relation > 1.05 || relation > 0.95) {
            int size = Math.min(width, heigth);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        }

        String storagePlace = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Android/data/" + c.getPackageName() + "/";
        String fileName = String.valueOf(System.currentTimeMillis()) + ".jpg";

        // saving cover
        int coverLength = getCoverLength(c);
        Bitmap cover = Bitmap.createScaledBitmap(bitmap, coverLength, coverLength, true);
        File coverDir = new File(storagePlace + "cover");
        File coverFile = new File(coverDir, fileName);
        //noinspection ResultOfMethodCallIgnored
        coverDir.mkdirs();
        try {
            FileOutputStream coverOut = new FileOutputStream(coverFile);
            cover.compress(Bitmap.CompressFormat.JPEG, 90, coverOut);
            coverOut.flush();
            coverOut.close();
            coverPath = coverFile.getAbsolutePath();
        } catch (IOException e) {
            L.d(TAG, e.getMessage());
            return null;
        }

        return coverPath;
    }

    public static boolean isOnline(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            boolean mobileConnectionAllowed = new PrefsManager(c).mobileConnectionAllowed();

            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return !(info.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnectionAllowed);
            }
        }
        return false;
    }

    public static Bitmap genBitmapFromFile(String pathName, Context c) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, c);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, Context c) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int reqLength = getCoverLength(c);

        //setting reqWidth matching to desired 1:1 ratio and screen-size
        if (width < height) {
            reqLength = (height / width) * reqLength;
        } else {
            reqLength = (width / height) * reqLength;
        }

        int inSampleSize = 1;

        if (height > reqLength || width > reqLength) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqLength
                    && (halfWidth / inSampleSize) > reqLength) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

