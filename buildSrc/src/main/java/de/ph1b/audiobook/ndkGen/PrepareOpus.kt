package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

@Suppress("unused")
open class PrepareOpus : DefaultTask() {

  var ndkDir: String = ""

  private val jniDir = File(project.projectDir, "src/main/jni")
  private val version = VERSION_OPUS

  @TaskAction
  fun prepare() {
    check(ndkDirExists()) { "Invalid ndkDir=$ndkDir" }
    val opusDir = File(jniDir, "libopus")
    if (validateLibrary(opusDir, version)) {
      logger.debug("Opus exists already.")
      return
    }
    opusDir.deleteRecursively()
    val dstFile = downloadArchive()
    val extractedFolder = extractArchive(dstFile)
    extractedFolder.renameTo(opusDir)
    build()
  }

  private fun build() {
    execute(
        command = "./convert_android_asm.sh",
        directory = jniDir
    )

    execute(
        command = "$ndkDir/ndk-build APP_ABI=\"mips64 mips x86_64 x86 arm64-v8a armeabi-v7a\" -j4",
        directory = jniDir
    )

    logger.debug("Pre-Build success")
  }

  private fun extractArchive(dstFile: File): File {
    execute(
        command = "tar -xzf \"${dstFile.absolutePath}\" -C \"${dstFile.parentFile.absolutePath}\""
    )
    val extractedFolder = File(jniDir, "opus-$version")
    createVersionFile(extractedFolder, version)
    logger.debug("Extracted successfully")
    return extractedFolder
  }

  private fun downloadArchive(): File {
    logger.debug("Download")
    val dstFile = File(jniDir, "opus-$version.tar.gz")
    if (!dstFile.exists()) {
      val uri = URI.create("https://ftp.osuosl.org/pub/xiph/releases/opus/opus-$version.tar.gz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")
    return dstFile
  }

  private fun ndkDirExists(): Boolean {
    return File(ndkDir).exists()
  }
}
