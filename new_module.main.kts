#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:4.2.0")
@file:CompilerOptions("-Xopt-in=kotlin.RequiresOptIn")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import java.io.File

class NewFeatureModule : CliktCommand(name = "Creates a new feature module") {

  private val name: String by argument("The new module name, i.e. :unicorn:wings")
    .validate { name ->
      val valid = name.startsWith(":") && name.all { it.isLetter() || it == ':' }
      if (!valid) {
        fail("Invalid module name")
      }
    }
  private val components: List<String> by lazy {
    name.removePrefix(":").split(":")
  }

  override fun run() {
    val moduleRoot = File(components.joinToString(separator = "/"))

    val buildGradle = File(moduleRoot, "build.gradle.kts")
    buildGradle.parentFile.mkdirs()
    buildGradle.writeText(gradleContent())

    val packageName = components.joinToString(separator = ".")
    listOf("main", "test").forEach { sourceSet ->
      val srcFolder = File(moduleRoot, "src/$sourceSet/kotlin/voice/${packageName.replace(".", "/")}")
      srcFolder.mkdirs()
    }

    addModuleToSettingsGradle()
  }

  private fun addModuleToSettingsGradle() {
    val settingsGradle = File("settings.gradle.kts")
    val lines = settingsGradle.readLines().toMutableList()
    if (lines.last().isBlank()) {
      lines.removeLast()
    }
    lines += """include("$name")"""
    settingsGradle.writeText(lines.joinToString(separator = "\n"))
  }

  private fun String.toSnakeCase(): String {
    return "([a-z])([A-Z]+)".toRegex()
      .replace(this, "$1_$2")
      .lowercase()
  }

  private fun gradleContent(): String {
    val plugins = buildList {
      add("voice.library")
    }

    return buildString {
      appendLine("plugins {")
      plugins.forEach { plugin ->
        appendLine("  id(\"$plugin\")")
      }
      appendLine("}")

      appendLine()
      appendLine(
        """
        dependencies {
          // todo
        }
        """.trimIndent(),
      )
    }
  }
}

NewFeatureModule().main(args)
