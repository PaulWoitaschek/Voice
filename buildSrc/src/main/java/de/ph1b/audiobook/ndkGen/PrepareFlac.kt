package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI


@Suppress("unused")
open class PrepareFlac : DefaultTask() {

  var ndkDir: String = ""

  @TaskAction
  fun prepare() {
    if (!File(ndkDir).exists())
      throw IllegalArgumentException("Invalid ndkDir $ndkDir")

    val jniDir = File(project.projectDir, "src/main/jni/")

    // if the flac sources already exist, skip
    val flacDir = File(jniDir, "flac")

    d(flacDir.absolutePath)
    if (flacDir.exists() && flacDir.listFiles()?.isNotEmpty() == true)
      return
    flacDir.delete()

    val flacVersion = "1.3.1"

    val dstFile = File(jniDir, "flac-$flacVersion.tar.xz")
    if (!dstFile.exists()) {
      val uri = URI.create("http://downloads.xiph.org/releases/flac/flac-$flacVersion.tar.xz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")

    // extract and rename it
    execute("tar -xvf ${dstFile.absolutePath} -C ${dstFile.parentFile.absolutePath}")
    val extractedFolder = File(jniDir, "flac-$flacVersion")
    extractedFolder.renameTo(flacDir)
    if (flacDir.listFiles()?.size ?: 0 <= 0)
      throw IllegalStateException("FlacDir is empty")

    execute(
        command = "$ndkDir/ndk-build APP_ABI=all -j4",
        timeOut = 60,
        directory = jniDir
    )
  }
}