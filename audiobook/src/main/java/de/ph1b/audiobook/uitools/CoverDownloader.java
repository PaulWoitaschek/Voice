package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.ph1b.audiobook.utils.L;

public class CoverDownloader {

    private static final String TAG = CoverDownloader.class.getSimpleName();
    private static final HashMap<String, List<String>> searchMapping = new HashMap<>();
    private final Picasso picasso;
    private Call call = null;

    public CoverDownloader(@NonNull Context c) {
        picasso = Picasso.with(c);
    }

    @NonNull
    private static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface i : interfaces) {
                List<InetAddress> internetAddresses = Collections.list(i.getInetAddresses());
                for (InetAddress a : internetAddresses) {
                    if (!a.isLoopbackAddress()) {
                        return a.getHostAddress().toUpperCase();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
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
    @Nullable
    public Bitmap getCover(@NonNull String searchText, int number) {
        String bitmapUrl = getBitmapUrl(searchText, number);
        try {
            L.v(TAG, "number=" + number + ", url=" + bitmapUrl);
            return picasso.load(bitmapUrl).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private String getBitmapUrl(@NonNull String searchText, int number) {
        if (searchMapping.containsKey(searchText)) {
            List<String> containing = searchMapping.get(searchText);
            if (number < containing.size()) {
                return containing.get(number);
            } else {
                int startPoint = containing.size();
                L.v(TAG, "looking for new set at startPoint=" + startPoint);
                List<String> newSet = getNewLinks(searchText, startPoint);
                if (newSet.size() > 0) {
                    containing.addAll(newSet);
                    return newSet.get(0);
                } else {
                    return null;
                }
            }
        } else {
            List<String> newSet = getNewLinks(searchText, 0);
            if (newSet.size() > 0) {
                searchMapping.put(searchText, newSet);
                return newSet.get(0);
            } else {
                return null;
            }
        }
    }

    private List<String> getNewLinks(@NonNull String searchText, int startPage) {
        List<String> newStrings = new ArrayList<>();

        try {
            Uri uri = new Uri.Builder()
                    .scheme("https")
                    .authority("ajax.googleapis.com")
                    .appendPath("ajax")
                    .appendPath("services")
                    .appendPath("search")
                    .appendPath("images")
                    .appendQueryParameter("v", "1.0")
                    .appendQueryParameter("imgsz", "large|xlarge")
                    .appendQueryParameter("rsz", "8")
                    .appendQueryParameter("q", searchText + " cover")
                    .appendQueryParameter("start", String.valueOf(startPage))
                    .appendQueryParameter("userip", getIPAddress())
                    .build();

            URL url = new URL(uri.toString());

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
        } catch (IOException | JSONException ignored) {
        }

        return newStrings;
    }
}