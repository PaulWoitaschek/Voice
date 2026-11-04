package voice.core.scanner

import java.io.File
import org.junit.rules.TemporaryFolder

// a named pipe can't be opened until something writes to it, like a cloud file whose download
// stalled
internal fun TemporaryFolder.newStalledFile(name: String): File {
  val file = File(root, name)
  ProcessBuilder("mkfifo", file.absolutePath).start().waitFor()
  return file
}
