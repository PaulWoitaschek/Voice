package de.ph1b.audiobook.features.chapterReader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A small part of apaches IOUtils. License is Apache 2.0
 *
 * @author Paul Woitaschek
 */
public final class IOUtils {

  /**
   * The default buffer size to use for the skip() methods.
   */
  private static final int SKIP_BUFFER_SIZE = 2048;
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
  private static byte[] SKIP_BYTE_BUFFER;

  private IOUtils() {}

  public static void skipFully(final InputStream input, final long toSkip) throws IOException {
    if (toSkip < 0) {
      throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
    }
    final long skipped = skip(input, toSkip);
    if (skipped != toSkip) {
      throw new EOFException("Bytes to skip: " + toSkip + " actual: " + skipped);
    }
  }

  private static long skip(final InputStream input, final long toSkip) throws IOException {
    if (toSkip < 0) {
      throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
    }

    /*
     * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple times (the data
     * is ignored) - we always use the same size buffer, so if it it is recreated it will still be OK (if the buffer
     * size were variable, we would need to synch. to ensure some other thread did not create a smaller one)
     */
    if (SKIP_BYTE_BUFFER == null) {
      SKIP_BYTE_BUFFER = new byte[SKIP_BUFFER_SIZE];
    }
    long remain = toSkip;
    while (remain > 0) {
      // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
      final long n = input.read(SKIP_BYTE_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
      if (n < 0) { // EOF
        break;
      }
      remain -= n;
    }
    return toSkip - remain;
  }
}
