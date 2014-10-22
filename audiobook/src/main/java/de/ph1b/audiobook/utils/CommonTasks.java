package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.NoExternalStorage;


public class CommonTasks {

    private static final String TAG = "CommonTasks";


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
        backgroundPaint.setColor(resources.getColor(R.color.colorAccent));
        canvas.drawRect(0, 0, pxSize, pxSize, backgroundPaint);
        int y = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
        canvas.drawText(bookName.substring(0, 1).toUpperCase(), pxSize / 2, y, textPaint);
        return thumb;
    }


    public static int getCoverSize(Context c) {
        c.getResources().getDisplayMetrics();
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        return width < height ? width : height;
    }

    public static int getThumbSize(Context c) {
        float thumbSizeDp = c.getResources().getDimension(R.dimen.thumb_size);
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, thumbSizeDp, Resources.getSystem().getDisplayMetrics()));
    }

    public static void checkExternalStorage(Context c) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Intent i = new Intent(c, NoExternalStorage.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(new Intent(i));
        }
    }

    public static String[] saveBitmap(Bitmap bitmap, Context c) {
        int width = bitmap.getWidth();
        int heigth = bitmap.getHeight();
        int relation = width / heigth;
        if (relation > 1.05 || relation > 0.95) {
            int size = Math.min(width, heigth);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        }
        int coverSize = getCoverSize(c);
        int thumbSize = getThumbSize(c);
        Bitmap cover = Bitmap.createScaledBitmap(bitmap, coverSize, coverSize, true);
        Bitmap thumb = Bitmap.createScaledBitmap(cover, thumbSize, thumbSize, true);

        String packageName = c.getPackageName();
        String fileName = String.valueOf(System.currentTimeMillis()) + ".png";

        File coverDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/cover");
        File thumbDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName + "/thumb");
        File coverFile = new File(coverDir, fileName);
        File thumbFile = new File(thumbDir, fileName);
        //noinspection ResultOfMethodCallIgnored
        coverDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        thumbDir.mkdirs();
        try {
            FileOutputStream coverOut = new FileOutputStream(coverFile);
            FileOutputStream thumbOut = new FileOutputStream(thumbFile);
            cover.compress(Bitmap.CompressFormat.PNG, 90, coverOut);
            thumb.compress(Bitmap.CompressFormat.PNG, 90, thumbOut);
            coverOut.flush();
            thumbOut.flush();
            coverOut.close();
            thumbOut.close();
            return new String[]{coverFile.getAbsolutePath(), thumbFile.getAbsolutePath()};
        } catch (IOException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
        }
        return null;
    }


    /*
    public static String[] saveCovers(Bitmap cover, Context c) {
        if (cover != null) {
            String thumbPath;
            String coverPath;

            int thumbSize = (int) convertDpToPx(c.getResources().getDimension(R.dimen.thumb_size));

            String packageName = c.getPackageName();
            String fileName = String.valueOf(System.currentTimeMillis()) + ".png";
            int coverDimensions = getSmallerDisplaySide(c);

            int relation = cover.getHeight() / cover.getWidth();
            if (relation > 1.05 || relation < 0.95) {

            }
            cover = Bitmap.createScaledBitmap(cover, coverDimensions, coverDimensions, false);

            Bitmap thumb = Bitmap.createScaledBitmap(cover, thumbSize, thumbSize, false);
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
                return new String[]{coverPath, thumbPath};
            } catch (IOException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
                return null;
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Cover was null, so returning null");
            return null;
        }
    }
    */

    public static boolean isOnline(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
            boolean mobileConnectionAllowed = sharedPref.getBoolean(c.getString(R.string.pref_cover_on_internet), false);

            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return !(info.getType() == ConnectivityManager.TYPE_MOBILE && !mobileConnectionAllowed);
            }
        }
        return false;
    }

    private static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface i : interfaces) {
                List<InetAddress> internetAdresses = Collections.list(i.getInetAddresses());
                for (InetAddress a : internetAdresses) {
                    if (!a.isLoopbackAddress()) {
                        String address = a.getHostAddress().toUpperCase();
                        if (InetAddressUtils.isIPv4Address(address))
                            return address;
                    }
                }
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
        }
        return "";
    }

    public static URLConnection connection = null;

    public static void deleteFile(File deleteFile) {
        if (deleteFile != null)
            deleteFile.deleteOnExit();
    }

    /*
    returns a bitmap from the internet.
    this bitmap is not scaled.
     */
    public static Bitmap genCoverFromInternet(String searchText, int pageCounter, Context c) {
        int connectTimeOut = 3000;
        int readTimeOut = 5000;
        searchText = searchText + " audiobook cover";
        InputStream inputStream;
        try {
            URL url = new URL(
                    "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&imgsz=large|xlarge&rsz=1&q=" +
                            URLEncoder.encode(searchText, "UTF-8") + "&start=" + pageCounter +
                            "&userip=" + getIPAddress());

            if (BuildConfig.DEBUG)
                Log.d(TAG, url.toString());
            connection = url.openConnection();
            connection.setReadTimeout(readTimeOut);
            connection.setConnectTimeout(connectTimeOut);

            inputStream = connection.getInputStream();

            String line;
            StringBuilder builder = new StringBuilder();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject obj = new JSONObject(builder.toString());
            JSONObject responseData = obj.getJSONObject("responseData");
            JSONArray results = responseData.getJSONArray("results");
            String imageUrl = results.getJSONObject(0).getString("url");
            if (imageUrl != null) {
                url = new URL(imageUrl);
                connection = url.openConnection();
                connection.setConnectTimeout(connectTimeOut);
                connection.setReadTimeout(readTimeOut);
                inputStream = connection.getInputStream();

                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, c);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                connection = url.openConnection();
                connection.setConnectTimeout(connectTimeOut);
                connection.setReadTimeout(readTimeOut);
                inputStream = connection.getInputStream();

                //returned bitmap in the desired 1:1 ratio and cropping automatically
                return BitmapFactory.decodeStream(inputStream, null, options);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, e.toString());
            return null;
        }

        return null;
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


    private static int calculateInSampleSize(
            BitmapFactory.Options options, Context c) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        int reqHeight;
        int reqWidth;

        //setting reqWidth matching to desired 1:1 ratio and screen-size
        if (width < height) {
            reqWidth = getCoverSize(c);
            reqHeight = (height / width) * reqWidth;
        } else {
            reqHeight = getCoverSize(c);
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

