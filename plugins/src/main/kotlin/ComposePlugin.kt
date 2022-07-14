@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// declared in the build.gradle.file
@Suppress("unused")
class ComposePlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.withPlugin("com.android.application") {
      target.extensions.configure(ApplicationExtension::class.java) { extension ->
        configureCompose(extension, target)
      }
    }
    target.pluginManager.withPlugin("com.android.library") {
      target.extensions.configure(LibraryExtension::class.java) { extension ->
        configureCompose(extension, target)
      }
    }
  }

  private fun configureCompose(extension: CommonExtension<*, *, *, *>, target: Project) {
    val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    target.dependencies.add("implementation", libs.findBundle("compose").get())
    extension.buildFeatures.compose = true
    extension.composeOptions {
      kotlinCompilerExtensionVersion = libs.findVersion("compose-compiler").get().requiredVersion
    }
    target.tasks.withType(KotlinCompile::class.java) { kotlinCompile ->
      kotlinCompile.kotlinOptions {
        allWarningsAsErrors = true
        freeCompilerArgs = freeCompilerArgs + listOf(
          "androidx.compose.material.ExperimentalMaterialApi",
          "androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
          "androidx.compose.material3.ExperimentalMaterial3Api",
          "androidx.compose.foundation.ExperimentalFoundationApi",
          "androidx.compose.ui.ExperimentalComposeUiApi",
          "androidx.compose.animation.ExperimentalAnimationApi"
        ).map { "-opt-in=$it" }
      }
    }
  }
}
