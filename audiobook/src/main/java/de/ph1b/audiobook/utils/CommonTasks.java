package de.ph1b.audiobook.utils;

import android.app.Activity;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

    public static String getIPAddress() {
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

    public static Bitmap genBitmapFromInternet(String searchText, int pageCounter) {
        int retryLeft = 3;
        while (retryLeft-- != 0) {
            int readTimeOut = 5000;
            int connectTimeOut = 5000;
            searchText = searchText + " audiobook cover";
            try {
                URL searchUrl = new URL(
                        "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&imgsz=large|xlarge&rsz=1&q=" +
                                URLEncoder.encode(searchText, "UTF-8") + "&start=" + pageCounter +
                                "&userip=" + getIPAddress());

                if (BuildConfig.DEBUG)
                    Log.d(TAG, searchUrl.toString());
                URLConnection connection = searchUrl.openConnection();
                connection.setReadTimeout(readTimeOut);
                connection.setConnectTimeout(connectTimeOut);

                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);

                }
                JSONObject obj = new JSONObject(builder.toString());
                JSONObject responseData = obj.getJSONObject("responseData");
                JSONArray results = responseData.getJSONArray("results");

                String imageUrl = results.getJSONObject(0).getString("url");

                if (imageUrl != null) {
                    URL url = new URL(imageUrl);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setConnectTimeout(connectTimeOut);
                    urlConnection.setReadTimeout(readTimeOut);
                    InputStream inputStream = urlConnection.getInputStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    byte[] outPut = buffer.toByteArray();
                    if (outPut.length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(outPut, 0, outPut.length);
                        if (bitmap != null)
                            return bitmap;
                    }
                }
            } catch (Exception e) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, e.toString());
            }
        }
        return null;
    }
}

