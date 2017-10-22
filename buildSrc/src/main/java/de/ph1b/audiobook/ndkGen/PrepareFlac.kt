package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

@Suppress("unused")
open class PrepareFlac : DefaultTask() {

  private val jniDir = File(project.projectDir, "src/main/jni")
  private val version: String = DependencyVersions.VERSION_FLAC

  @TaskAction
  fun prepare() {
    val flacDir = File(jniDir, "flac")
    if (validateLibrary(flacDir, version)) {
      return
    }
    flacDir.deleteRecursively()
    val dstFile = downloadArchive()
    extractArchive(dstFile, flacDir)
  }

  private fun downloadArchive(): File {
    val dstFile = File(jniDir, "flac-$version.tar.xz")
    if (!dstFile.exists()) {
      val uri = URI.create("https://ftp.osuosl.org/pub/xiph/releases/flac/flac-$version.tar.xz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")
    return dstFile
  }

  private fun extractArchive(dstFile: File, flacDir: File) {
    execute("tar -xvf \"${dstFile.absolutePath}\" -C \"${dstFile.parentFile.absolutePath}\"")
    val extractedFolder = File(jniDir, "flac-$version")
    createVersionFile(extractedFolder, version)
    extractedFolder.renameTo(flacDir)
    if (flacDir.listFiles()?.size ?: 0 <= 0)
      throw IllegalStateException("FlacDir is empty")
  }
}
