package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.Compiler.command
import java.net.URI


@Suppress("unused")
open class PrepareFlac : DefaultTask() {

  var ndkDir: String = ""

  @TaskAction
  fun prepare() {
    if (!File(ndkDir).exists())
      throw IllegalArgumentException("Invalid ndkDir $ndkDir")

    // if the flac sources already exist, skip
    val flacDir = file("src/main/jni/flac")

    d(flacDir.absolutePath)
    if (flacDir.exists() && flacDir.listFiles()?.isNotEmpty() == true)
      return
    flacDir.delete()


    val flacVersion = "1.3.1"

    val dstFile = file("src/main/jni/flac-$flacVersion.tar.xz")
    if (!dstFile.exists()) {
      val uri = URI.create("http://downloads.xiph.org/releases/flac/flac-$flacVersion.tar.xz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")

    // extract and rename it
    command("tar -xvf ${dstFile.absolutePath} -C ${dstFile.parentFile.absolutePath}")
    val extractedFolder = file("src/main/jni/flac-$flacVersion")
    extractedFolder.renameTo(flacDir)

    command("cd ${file("src/main/jni")} && $ndkDir/ndk-build APP_ABI=all -j4", 60)
  }
}