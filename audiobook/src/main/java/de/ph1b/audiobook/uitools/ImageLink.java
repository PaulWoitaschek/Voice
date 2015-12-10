package de.ph1b.audiobook.uitools;

import android.support.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple class for Retrofit that represents an image query.
 *
 * @author Paul Woitaschek
 */
final class ImageLink {

    @Nullable
    @SerializedName("responseData")
    public ResponseData responseData;

    public List<String> urls() {
        if (responseData == null || responseData.results == null) {
            return Collections.emptyList();
        } else {
            List<String> urls = new ArrayList<>(responseData.results.size());
            for (ResponseData.Result r : responseData.results) {
                if (r != null) {
                    urls.add(r.url);
                }
            }
            return urls;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("urls", urls())
                .toString();
    }

    public static final class ResponseData {

        @Nullable
        @SerializedName("results")
        public List<ResponseData.Result> results;

        public static final class Result {

            @Nullable
            @SerializedName("url")
            public String url;
        }
    }
}
