package de.ph1b.audiobook.utils;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CoverDownloader {

    private static final String TAG = CoverDownloader.class.getSimpleName();
    private static final HashMap<String, ArrayList<String>> searchMapping = new HashMap<>();
    private final Picasso picasso;
    private Call call = null;

    public CoverDownloader(Context c) {
        picasso = Picasso.with(c);
    }

    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * Returns a bitmap from google, defined by the searchText
     *
     * @param searchText The Audiobook to look for
     * @param number     The nth result
     * @return the generated bitmap. If no bitmap was found, returns null
     */
    public Bitmap getCover(String searchText, int number) {
        String bitmapUrl = getBitmapUrl(searchText, number);
        try {
            L.v(TAG, "number=" + number + ", url=" + bitmapUrl);
            return picasso.load(bitmapUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getIPAddress() {
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

    private String getBitmapUrl(String searchText, int number) {
        if (searchMapping.containsKey(searchText)) {
            ArrayList<String> containing = searchMapping.get(searchText);
            if (number < containing.size()) {
                return containing.get(number);
            } else {
                int startPoint = containing.size();
                L.v(TAG, "looking for new set at startPoint=" + startPoint);
                ArrayList<String> newSet = getNewLinks(searchText, startPoint);
                if (newSet.size() > 0) {
                    containing.addAll(newSet);
                    return newSet.get(0);
                } else {
                    return null;
                }
            }
        } else {
            ArrayList<String> newSet = getNewLinks(searchText, 0);
            if (newSet.size() > 0) {
                searchMapping.put(searchText, newSet);
                return newSet.get(0);
            } else {
                return null;
            }
        }
    }

    private ArrayList<String> getNewLinks(String searchText, int startPage) {
        ArrayList<String> newStrings = new ArrayList<>();

        searchText = searchText + " cover";
        try {
            String url = "https://ajax.googleapis.com/ajax/services/search/" +
                    "images?v=1.0&imgsz=large%7Cxlarge&rsz=8&q=" +
                    URLEncoder.encode(searchText, "UTF-8") + //query
                    "&start=" + startPage + //start-page
                    "&userip=" + getIPAddress(); //ip

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            call = client.newCall(request);
            Response response = call.execute();
            JSONObject jsonObject = new JSONObject(response.body().string());
            JSONObject responseData = jsonObject.getJSONObject("responseData");
            JSONArray results = responseData.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                newStrings.add(results.getJSONObject(i).getString("url"));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return newStrings;
    }
}