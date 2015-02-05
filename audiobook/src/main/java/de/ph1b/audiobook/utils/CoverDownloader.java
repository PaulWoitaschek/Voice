package de.ph1b.audiobook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CoverDownloader {


    private static final String TAG = "CoverDownloader";

    private static final HashMap<String, ArrayList<URL>> searchStringMap = new HashMap<>();
    private static URLConnection connection = null;
    private static int connectTimeOut;
    private static int readTimeOut;

    public static void cancelDownload() {
        if (connection != null) {
            try {
                connection.getInputStream().close();
            } catch (Exception ignore) {
            }
        }
        connectTimeOut = 0;
        readTimeOut = 0;
    }

    /**
     * Returns a bitmap from google, defined by the searchText
     *
     * @param searchText The Audiobook to look for
     * @param c          Application context
     * @param number     The nth result
     * @return the generated bitmap. If no bitmap was found, returns null
     */
    public static Bitmap getCover(String searchText, Context c, int number) {

        connectTimeOut = 3000;
        readTimeOut = 5000;

        if (number > 64) {
            L.d(TAG, "Number exceeded results: " + number);
            return null;
        }

        L.d(TAG, "Loading Cover with " + searchText + " and #" + String.valueOf(number));

        ArrayList<URL> bitmapUrls;

        URL searchURL;

        // if there is a value corresponding to searchText, use that one
        if (searchStringMap.containsKey(searchText)) {
            L.d(TAG, "Found bitmapUrls");
            bitmapUrls = searchStringMap.get(searchText);
            if (number < bitmapUrls.size()) {
                searchURL = bitmapUrls.get(number);
                L.d(TAG, "Already got one in cache!:" + searchURL);
            } else {
                L.d(TAG, "Will look for a new eightSet because bitmapUrls is too small");
                ArrayList<URL> newSetOfURL = new ArrayList<>();
                newSetOfURL.addAll(bitmapUrls);
                ArrayList<URL> newUrls = genNewURLs(searchText, bitmapUrls.size() + 1);
                if (newUrls == null)
                    return null;
                newSetOfURL.addAll(newUrls);
                searchStringMap.put(searchText, newSetOfURL);
                searchURL = newSetOfURL.get(0);
                L.d(TAG, "Got one: " + searchURL);
            }
        } else {
            L.d(TAG, "Didn't find bitmapUrls");
            ArrayList<URL> newUrls = genNewURLs(searchText, number);
            if (newUrls.size() == 0) {
                return null;
            }
            searchStringMap.put(searchText, newUrls);
            searchURL = newUrls.get(0);
            L.d(TAG, "But made a new one, returning:" + searchURL);
        }


        InputStream inputStream;

        if (searchURL != null) {
            try {
                connection = searchURL.openConnection();
                connection.setConnectTimeout(connectTimeOut);
                connection.setReadTimeout(readTimeOut);
                inputStream = connection.getInputStream();

                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);

                // Calculate inSampleSize
                Rect reqSize = ImageHelper.resolveImageType(ImageHelper.TYPE_COVER, c);
                options.inSampleSize = ImageHelper.calculateInSampleSize(reqSize, options);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                connection = searchURL.openConnection();
                connection.setConnectTimeout(connectTimeOut);
                connection.setReadTimeout(readTimeOut);
                inputStream = connection.getInputStream();

                //returned bitmap in the desired 1:1 ratio and cropping automatically
                return BitmapFactory.decodeStream(inputStream, null, options);
            } catch (IOException e) {
                L.d(TAG, "Catched IOException!", e);
            }
        }
        return null;
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
            L.d(TAG, e.getMessage());
        }
        return "";
    }


    /**
     * Gens a new set of urls pointing to covers.
     *
     * @param searchText The name of the audiobook
     * @param startPage  the (google-) page to begin with
     * @return the new URLs
     */
    private static ArrayList<URL> genNewURLs(String searchText, int startPage) {
        searchText = searchText + " cover";
        ArrayList<URL> urls = new ArrayList<>();

        try {
            String url = "https://ajax.googleapis.com/ajax/services/search/" +
                    "images?v=1.0&imgsz=large%7Cxlarge&rsz=8&q=" +
                    URLEncoder.encode(searchText, "UTF-8") + //query
                    "&start=" + startPage + //startpage
                    "&userip=" + getIPAddress(); //ip
            
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONObject responseData = jsonObject.getJSONObject("responseData");
            JSONArray results = responseData.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                urls.add(new URL(results.getJSONObject(i).getString("url")));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return urls;
    }
}