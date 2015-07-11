package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.ph1b.audiobook.utils.L;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class ImageHelper {

    private static final String TAG = ImageHelper.class.getSimpleName();

    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


    /**
     * Saves a bitmap as a file to the personal directory.
     *
     * @param bitmap The bitmap to be saved
     * @param c      Application context
     */
    public static synchronized void saveCover(@NonNull Bitmap bitmap, Context c, @NonNull File destination) {
        // make bitmap square
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        if (width != height) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        }

        // scale down if bitmap is too large
        int preferredSize = getSmallerScreenSize(c);
        if (size > preferredSize) {
            bitmap = Bitmap.createScaledBitmap(bitmap, preferredSize, preferredSize, true);
        }

        // save bitmap to storage
        try {
            FileOutputStream coverOut = new FileOutputStream(destination);
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, coverOut);
                coverOut.flush();
            } finally {
                coverOut.close();
            }
        } catch (IOException e) {
            L.e(TAG, "Error at saving image with destination=" + destination, e);
        }
    }

    @SuppressWarnings("deprecation")
    public static int getSmallerScreenSize(Context c) {
        Display display = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int displayWidth = display.getWidth();
        int displayHeight = display.getHeight();
        return displayWidth < displayHeight ? displayWidth : displayHeight;
    }

    public static boolean isOnline(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Bitmap getEmbeddedCover(File f, Context c) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(f.getAbsolutePath());
            byte[] data = mmr.getEmbeddedPicture();
            if (data != null) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, c);
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeByteArray(data, 0, data.length, options);
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, Context c) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int reqLength = getSmallerScreenSize(c);

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

