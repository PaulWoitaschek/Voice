package de.ph1b.audiobook.ndkGen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.util.*

@Suppress("unused")
open class PrepareOpus : DefaultTask() {

  @TaskAction
  fun prepare() {

    // if the opus sources already exist, skip
    val opusDir = file("src/main/jni/libopus")

    d(opusDir.absolutePath)
    if (opusDir.exists() && opusDir.listFiles()?.isNotEmpty() == true)
      return
    opusDir.delete()

    val opusVersion = "1.1.4"

    val dstFile = file("src/main/jni/opus-$opusVersion.tar.gz")
    if (!dstFile.exists()) {
      val uri = URI.create("http://downloads.xiph.org/releases/opus/opus-$opusVersion.tar.gz")
      download(uri, dstFile)
    }
    d("downloaded to $dstFile")

    // extract and rename it
    command("tar -xzf ${dstFile.absolutePath} -C ${dstFile.parentFile.absolutePath}")
    val extractedFolder = file("src/main/jni/opus-$opusVersion")
    extractedFolder.renameTo(opusDir)

    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())
    val ndkDir = properties.getProperty("ndk.dir")

    command(file("src/main/jni/convert_android_asm.sh").absolutePath)

    command("cd ${file("src/main/jni")} && $ndkDir/ndk-build APP_ABI=all -j4", 60)
  }
}