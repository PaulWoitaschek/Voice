package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

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

/**
 * Helper class for downloading covers from the internet.
 */
public class CoverDownloader {

    private static final String TAG = CoverDownloader.class.getSimpleName();
    private static final HashMap<String, List<String>> SEARCH_MAPPING = new HashMap<>(10);
    private final Picasso picasso;
    private Call call = null;

    public CoverDownloader(@NonNull Context c) {
        picasso = Picasso.with(c);
    }

    /**
     * @return the ip address or an empty String if none was found
     */
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Cancels the cover downloading if one is in progress
     */
    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * Fetches a cover into Picassos internal cache and returns the url if that worked.
     *
     * @param searchText The Audiobook to look for
     * @param number     The nth result
     * @return the generated bitmap. If no bitmap was found, returns null
     */
    @Nullable
    public String fetchCover(@NonNull String searchText, int number) {
        String bitmapUrl = getBitmapUrl(searchText, number);
        try {
            L.v(TAG, "number=" + number + ", url=" + bitmapUrl);
            Bitmap bitmap = picasso.load(bitmapUrl).get();
            if (bitmap != null) {
                return bitmapUrl;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param searchText The text to search the cover by
     * @param number     The nth cover with the given searchText. Starts at 0
     * @return The URL of the cover found or <code>null</code> if none was found
     */
    @Nullable
    private String getBitmapUrl(@NonNull String searchText, int number) {
        if (SEARCH_MAPPING.containsKey(searchText)) {
            List<String> containing = SEARCH_MAPPING.get(searchText);
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
                SEARCH_MAPPING.put(searchText, newSet);
                return newSet.get(0);
            } else {
                return null;
            }
        }
    }

    /**
     * Queries google for new urls of images
     *
     * @param searchText The Text to search the cover by
     * @param startPage  The start number for the covers. If the last time this returned an array
     *                   with the size of 8 the next time this number should be increased by excactly
     *                   that amount + 1.
     * @return A list of urls with the new covers. Might be empty
     */
    @NonNull
    @Size(min = 0)
    private List<String> getNewLinks(@NonNull String searchText, int startPage) {
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

            List<String> newStrings = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++) {
                newStrings.add(results.getJSONObject(i).getString("url"));
            }
            return newStrings;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(0);
    }
}