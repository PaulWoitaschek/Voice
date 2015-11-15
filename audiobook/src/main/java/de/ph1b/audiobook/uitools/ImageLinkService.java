package de.ph1b.audiobook.uitools;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * Service for retrofit for receiving an observable on new image links.
 *
 * @author Paul Woitaschek
 */
public interface ImageLinkService {
    @GET("images?v=1.0&imgsz=large%7Cxlarge&rsz=8")
    Observable<ImageLink> imageLinks(@Query("q") String bookName,
                                     @Query("start") int startPage,
                                     @Query("userip") String ip);
}
