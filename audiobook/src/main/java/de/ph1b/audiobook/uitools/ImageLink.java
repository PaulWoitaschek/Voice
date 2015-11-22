package de.ph1b.audiobook.uitools;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class for Retrofit that represents an image query.
 *
 * @author Paul Woitaschek
 */
final class ImageLink {

    @SerializedName("responseData")
    private final ResponseData responseData;

    public ImageLink(ResponseData responseData) {
        this.responseData = responseData;
    }

    public List<String> urls() {
        List<String> urls = new ArrayList<>(responseData.results.size());
        for (ResponseData.Result r : responseData.results) {
            urls.add(r.url);
        }
        return urls;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("urls", urls())
                .toString();
    }

    public static final class ResponseData {
        @SerializedName("results")
        private final List<ResponseData.Result> results;

        public ResponseData(List<ResponseData.Result> results) {
            this.results = results;
        }


        public static final class Result {
            @SerializedName("url")
            private final String url;

            public Result(String url) {
                this.url = url;
            }
        }
    }
}
