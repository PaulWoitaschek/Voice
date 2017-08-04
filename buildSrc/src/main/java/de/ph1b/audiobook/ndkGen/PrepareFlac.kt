package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI


@Suppress("unused")
open class PrepareFlac : DefaultTask() {

  @TaskAction
  fun prepare() {

    val jniDir = File(project.projectDir, "src/main/jni/")

    // if the flac sources already exist, skip
    val flacDir = File(jniDir, "flac")
    if (flacDir.listFilesSafely().isNotEmpty())
      return
    flacDir.delete()

    val dstFile = File(jniDir, "flac-$VERSION_FLAC.tar.xz")
    if (!dstFile.exists()) {
      val uri = URI.create("https://ftp.osuosl.org/pub/xiph/releases/flac/flac-$VERSION_FLAC.tar.xz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")

    // extract and rename it
    execute("tar -xvf \"${dstFile.absolutePath}\" -C \"${dstFile.parentFile.absolutePath}\"")
    val extractedFolder = File(jniDir, "flac-$VERSION_FLAC")
    extractedFolder.renameTo(flacDir)
    if (flacDir.listFiles()?.size ?: 0 <= 0)
      throw IllegalStateException("FlacDir is empty")
  }
}
