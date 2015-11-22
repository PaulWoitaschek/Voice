package de.ph1b.audiobook.uitools;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.interfaces.ForApplication;
import timber.log.Timber;

/**
 * Helper class for downloading covers from the internet.
 *
 * @author Paul Woitaschek
 */
@Singleton
public final class CoverDownloader {

    private static final HashMap<String, List<String>> SEARCH_MAPPING = new HashMap<>(10);
    private final Picasso picasso;
    private final ImageLinkService imageLinkService;

    @Inject
    public CoverDownloader(@NonNull @ForApplication Context c, @NonNull ImageLinkService imageLinkService) {
        picasso = Picasso.with(c);
        this.imageLinkService = imageLinkService;
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
            Timber.v("number=%d, url=%s", number, bitmapUrl);
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
     * @return The URL of the cover found or {@code null} if none was found
     */
    @Nullable
    private String getBitmapUrl(@NonNull String searchText, int number) {
        if (SEARCH_MAPPING.containsKey(searchText)) {
            List<String> containing = SEARCH_MAPPING.get(searchText);
            if (number < containing.size()) {
                return containing.get(number);
            } else {
                int startPoint = containing.size();
                Timber.v("looking for new set at startPoint %d", startPoint);
                List<String> newSet = getNewLinks(searchText, startPoint);
                if (!newSet.isEmpty()) {
                    containing.addAll(newSet);
                    return newSet.get(0);
                } else {
                    return null;
                }
            }
        } else {
            List<String> newSet = getNewLinks(searchText, 0);
            if (!newSet.isEmpty()) {
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
        searchText += " cover";
        return imageLinkService.imageLinks(searchText, startPage, getIPAddress())
                .toBlocking()
                .single()
                .urls();
    }
}