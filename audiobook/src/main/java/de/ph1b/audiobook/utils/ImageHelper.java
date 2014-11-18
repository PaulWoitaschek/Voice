package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookChooseFragment;


public class ImageHelper {

    private static final String TAG = "CommonTasks";


    public static Bitmap genCapital(String bookName, Context c, int type) {
        Rect rect;
        switch (type) {
            case TYPE_COVER:
                rect = resolveImageType(TYPE_COVER, c);
                break;
            case TYPE_MEDIUM:
                rect = resolveImageType(TYPE_MEDIUM, c);
                break;
            case TYPE_THUMB:
                rect = resolveImageType(TYPE_THUMB, c);
                break;
            case TYPE_NOTIFICATION:
                rect = resolveImageType(TYPE_NOTIFICATION, c);
                break;
            default:
                return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint textPaint = new Paint();
        textPaint.setTextSize(2 * rect.height() / 3);
        textPaint.setColor(c.getResources().getColor(android.R.color.white));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(c.getResources().getColor(R.color.colorAccent));
        canvas.drawRect(0, 0, rect.width(), rect.height(), backgroundPaint);
        int y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
        canvas.drawText(bookName.substring(0, 1).toUpperCase(), rect.height() / 2, y, textPaint);
        return bitmap;
    }

    public static Rect resolveImageType(int type, Context c) {
        c.getResources().getDisplayMetrics();
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;
        int squareCover = displayWidth < displayHeight ? displayWidth : displayHeight;

        switch (type) {
            case TYPE_COVER:
                return new Rect(0, 0, squareCover, squareCover);
            case TYPE_MEDIUM:
                float columns = BookChooseFragment.getAmountOfColumns(c);
                return new Rect(0, 0, Math.round(squareCover / columns), Math.round(squareCover / columns));
            case TYPE_THUMB:
                int thumbSizePx = c.getResources().getDimensionPixelSize(R.dimen.thumb_size);
                return new Rect(0, 0, thumbSizePx, thumbSizePx);
            case TYPE_NOTIFICATION:
                int height = c.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
                int width = c.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                return new Rect(0, 0, width, height);
            default:
                return null;
        }
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
        Rect coverSize = resolveImageType(TYPE_COVER, c);
        Bitmap cover = Bitmap.createScaledBitmap(bitmap, coverSize.width(), coverSize.height(), true);
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
            if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
            return null;
        }

        return coverPath;
    }

    public static boolean isOnline(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            boolean mobileConnectionAllowed = sharedPref.getBoolean(c.getString(R.string.pref_key_cover_on_internet), false);

            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return !(info.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnectionAllowed);
            }
        }
        return false;
    }


    public static final int TYPE_COVER = 0;
    public static final int TYPE_MEDIUM = 1;
    public static final int TYPE_THUMB = 2;
    public static final int TYPE_NOTIFICATION = 3;

    public static Bitmap genBitmapFromFile(String pathName, Context c, int type) {
        Rect reqSize = resolveImageType(type, c);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(reqSize, options);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }


    public static int calculateInSampleSize(Rect reqRect, BitmapFactory.Options options) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int reqWidth = reqRect.width();
        int reqHeight = reqRect.height();

        //setting reqWidth matching to desired 1:1 ratio and screen-size
        if (width < height) {
            reqHeight = (height / width) * reqWidth;
        } else {
            reqWidth = (width / height) * reqHeight;
        }

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

