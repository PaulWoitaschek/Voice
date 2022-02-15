@file:Suppress("UnstableApiUsage")

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion

val VersionCatalog.composeVersion: String get() = findVersion("composeCompiler").get().requiredVersion

val VersionCatalog.desugar: Provider<MinimalExternalModuleDependency> get() = findLibrary("desugar").get()

object VoiceVersions {
  const val compileSdk = 31
  const val minSdk = 24
  const val targetSdk = 30
  val javaCompileVersion = JavaVersion.VERSION_11
  val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(11)
}
