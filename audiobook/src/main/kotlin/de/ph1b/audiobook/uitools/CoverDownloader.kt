package de.ph1b.audiobook.uitools

import android.content.Context
import android.support.annotation.Size
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.IOException
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for downloading covers from the internet.

 * @author Paul Woitaschek
 */
@Singleton
class CoverDownloader
@Inject
constructor(c: Context, private val imageLinkService: ImageLinkService) {
    private val picasso: Picasso

    init {
        picasso = Picasso.with(c)
    }

    /**
     * Fetches a cover into Picassos internal cache and returns the url if that worked.

     * @param searchText The Audiobook to look for
     * *
     * @param number     The nth result
     * *
     * @return the generated bitmap. If no bitmap was found, returns null
     */
    fun fetchCover(searchText: String, number: Int): String? {
        val bitmapUrl = getBitmapUrl(searchText, number)
        try {
            Timber.v("number=%d, url=%s", number, bitmapUrl)
            val bitmap = picasso.load(bitmapUrl).get()
            if (bitmap != null) {
                return bitmapUrl
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * @param searchText The text to search the cover by
     * *
     * @param number     The nth cover with the given searchText. Starts at 0
     * *
     * @return The URL of the cover found or `null` if none was found
     */
    private fun getBitmapUrl(searchText: String, number: Int): String? {
        if (SEARCH_MAPPING.containsKey(searchText)) {
            val containing = SEARCH_MAPPING[searchText]!!
            if (number < containing.size) {
                return containing.get(number)
            } else {
                val startPoint = containing.size
                Timber.v("looking for new set at startPoint %d", startPoint)
                val newSet = getNewLinks(searchText, startPoint)
                if (!newSet.isEmpty()) {
                    containing.addAll(newSet)
                    return newSet[0]
                } else {
                    return null
                }
            }
        } else {
            val newSet = getNewLinks(searchText, 0)
            if (!newSet.isEmpty()) {
                SEARCH_MAPPING.put(searchText, ArrayList(newSet))
                return newSet[0]
            } else {
                return null
            }
        }
    }

    /**
     * Queries google for new urls of images

     * @param searchText The Text to search the cover by
     * *
     * @param startPage  The start number for the covers. If the last time this returned an array
     * *                   with the size of 8 the next time this number should be increased by excactly
     * *                   that amount + 1.
     * *
     * @return A list of urls with the new covers. Might be empty
     */
    @Size(min = 0)
    private fun getNewLinks(searchText: String, startPage: Int): List<String> {
        var searchFor = searchText
        searchFor += " cover"
        return imageLinkService.imageLinks(searchFor, startPage, ipAddress).toBlocking().single().urls()
    }

    companion object {

        private val SEARCH_MAPPING = HashMap<String, MutableList<String>>(10)

        /**
         * @return the ip address or an empty String if none was found
         */
        private val ipAddress: String
            get() {
                try {
                    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                    for (i in interfaces) {
                        val internetAddresses = Collections.list(i.inetAddresses)
                        for (a in internetAddresses) {
                            if (!a.isLoopbackAddress) {
                                return a.hostAddress.toUpperCase()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return ""
            }
    }
}