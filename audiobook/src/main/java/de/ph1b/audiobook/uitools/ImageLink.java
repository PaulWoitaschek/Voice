/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.uitools;

import android.support.annotation.Nullable;

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
        return "ImageLink{" +
                "responseData=" + responseData +
                '}';
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
