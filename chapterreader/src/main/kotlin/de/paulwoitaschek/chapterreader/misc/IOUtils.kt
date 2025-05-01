package de.paulwoitaschek.chapterreader.misc

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * A small part of apaches IOUtils. License is Apache 2.0
 */
internal object IOUtils {

  /**
   * The default buffer size to use for the skip() methods.
   */
  private const val SKIP_BUFFER_SIZE = 2048

  // Allocated in the relevant skip method if necessary.
    /*
     * These buffers are static and are shared between threads.
     * This is possible because the buffers are write-only - the contents are never read.
     *
     * N.B. there is no need to synchronize when creating these because:
     * - we don't care if the buffer is created multiple times (the data is ignored)
     * - we always use the same size buffer, so if it it is recreated it will still be OK
     * (if the buffer size were variable, we would need to synch. to ensure some other thread
     * did not create a smaller one)
     */
  private var SKIP_BYTE_BUFFER: ByteArray? = null

  @Throws(IOException::class)
  fun skipFully(
    input: InputStream,
    toSkip: Long,
  ) {
    if (toSkip < 0) {
      throw IllegalArgumentException("Bytes to skip must not be negative: $toSkip")
    }
    val skipped = skip(input, toSkip)
    if (skipped != toSkip) {
      throw EOFException("Bytes to skip: $toSkip actual: $skipped")
    }
  }

  @Throws(IOException::class)
  private fun skip(
    input: InputStream,
    toSkip: Long,
  ): Long {
    if (toSkip < 0) {
      throw IllegalArgumentException("Skip count must be non-negative, actual: $toSkip")
    }

        /*
         * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple times (the data
         * is ignored) - we always use the same size buffer, so if it it is recreated it will still be OK (if the buffer
         * size were variable, we would need to synch. to ensure some other thread did not create a smaller one)
         */
    if (SKIP_BYTE_BUFFER == null) {
      SKIP_BYTE_BUFFER = ByteArray(SKIP_BUFFER_SIZE)
    }
    var remain = toSkip
    while (remain > 0) {
      // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
      val n = input.read(SKIP_BYTE_BUFFER, 0, minOf(remain, SKIP_BUFFER_SIZE.toLong()).toInt())
      if (n < 0) { // EOF
        break
      }
      remain -= n
    }
    return toSkip - remain
  }
}
