package de.ph1b.audiobook.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.ph1b.audiobook.BuildConfig;

public class CoverDownloader {


    private static final String TAG = "CoverDownloader";

    private static final HashMap<String, ArrayList<URL>> searchStringMap = new HashMap<String, ArrayList<URL>>();
    private static URLConnection connection = null;
    private static int connectTimeOut;
    private static int readTimeOut;

    public static void cancelDownload(){
        if (connection != null){
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
            Log.d(TAG, "Number exceeded results: " + number);
            return null;
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Loading Cover with " + searchText + " and #" + String.valueOf(number));

        ArrayList<URL> bitmapUrls;

        URL searchURL;

        // if there is a value corresponding to searchText, use that one
        if (searchStringMap.containsKey(searchText)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Found bitmapUrls");
            bitmapUrls = searchStringMap.get(searchText);
            if (number < bitmapUrls.size()) {
                searchURL = bitmapUrls.get(number);
                if (BuildConfig.DEBUG) Log.d(TAG, "Already got one in cache!:" + searchURL);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Will look for a new eightSet because bitmapUrls is too small");
                ArrayList<URL> newSetOfURL = new ArrayList<URL>();
                newSetOfURL.addAll(bitmapUrls);
                ArrayList<URL> newUrls = genNewURLs(searchText, bitmapUrls.size() + 1);
                if (newUrls == null)
                    return null;
                newSetOfURL.addAll(newUrls);
                searchStringMap.put(searchText, newSetOfURL);
                searchURL = newSetOfURL.get(0);
                if (BuildConfig.DEBUG) Log.d(TAG, "Got one: " + searchURL);
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Didn't find bitmapUrls");
            ArrayList<URL> newUrls = genNewURLs(searchText, number);
            if (newUrls == null)
                return null;
            searchStringMap.put(searchText, newUrls);
            searchURL = newUrls.get(0);
            if (BuildConfig.DEBUG) Log.d(TAG, "But made a new one, returning:" + searchURL);
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
                if (BuildConfig.DEBUG) Log.d(TAG, "Catched IOException!");
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
            if (BuildConfig.DEBUG) Log.d(TAG, e.getMessage());
        }
        return "";
    }


    /**
     * Gens a new set of urls pointing to covers.
     *
     * @param searchText The name of the audiobook
     * @param startPage  the (google-) page to begin with
     * @return the new URLs, or <code>null</code> if none were found
     */
    private static ArrayList<URL> genNewURLs(String searchText, int startPage) {
        readTimeOut = 5000;
        connectTimeOut = 3000;

        searchText = searchText + " audiobook cover";

        ArrayList<URL> eightSetOfURL = new ArrayList<URL>();

        try {
            URL url = new URL(
                    "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&imgsz=large|xlarge&rsz=8&q=" +
                            URLEncoder.encode(searchText, "UTF-8") + "&start=" + startPage +
                            "&userip=" + getIPAddress());
            if (BuildConfig.DEBUG)
                Log.d(TAG, url.toString());
            connection = url.openConnection();
            connection.setReadTimeout(readTimeOut);
            connection.setConnectTimeout(connectTimeOut);

            InputStream inputStream = connection.getInputStream();

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
            for (int i = 0; i < results.length(); i++) {
                URL imageURL = new URL(results.getJSONObject(i).getString("url"));
                eightSetOfURL.add(imageURL);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
        }
        return eightSetOfURL.size() > 0 ? eightSetOfURL : null;
    }

}