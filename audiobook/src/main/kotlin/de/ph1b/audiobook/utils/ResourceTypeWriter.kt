package de.ph1b.audiobook.utils

import android.content.Context
import android.support.annotation.RawRes
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import rx.Observable
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple helper for converting raw files to string output

 * @author Paul Woitaschek
 */
@Singleton class ResourceTypeWriter
@Inject constructor(private val context: Context) {

    fun rawToString(@RawRes rawRes: Int): Observable<String> {
        // defer so this can be run async
        return Observable.defer {
            val inputStream = context.resources.openRawResource(rawRes)
            val streamReader = InputStreamReader(inputStream, Charsets.UTF_8)
            // closes streamReader properly
            streamReader.use {
                Observable.just(CharStreams.toString(streamReader))
            }
        }
    }
}