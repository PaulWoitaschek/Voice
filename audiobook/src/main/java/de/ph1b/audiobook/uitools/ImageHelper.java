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
import java.util.ArrayList;

import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.utils.L;

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
    public static void saveCover(Bitmap bitmap, Context c, @NonNull String root, @NonNull ArrayList<Chapter> chapters) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int relation = width / height;
        if (relation > 1.05 || relation > 0.95) {
            int size = Math.min(width, height);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        }

        // saving cover
        int coverLength = getSmallerScreenSize(c);
        Bitmap cover = Bitmap.createScaledBitmap(bitmap, coverLength, coverLength, true);
        File coverFile = Book.getCoverFile(root, chapters);
        if (coverFile.exists() && coverFile.canWrite()) {
            //noinspection ResultOfMethodCallIgnored
            coverFile.delete();
        }
        try {
            FileOutputStream coverOut = new FileOutputStream(coverFile);
            cover.compress(Bitmap.CompressFormat.JPEG, 90, coverOut);
            coverOut.flush();
            coverOut.close();
        } catch (IOException e) {
            L.e(TAG, e.getMessage());
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
        } catch (RuntimeException e) {
            e.printStackTrace();
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

