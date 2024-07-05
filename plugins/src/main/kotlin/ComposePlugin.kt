@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ComposePlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.withPlugin("com.android.application") {
      target.extensions.configure<ApplicationExtension> {
        configureCompose(this, target)
      }
    }
    target.pluginManager.withPlugin("com.android.library") {
      target.extensions.configure<LibraryExtension> {
        configureCompose(this, target)
      }
    }
  }

  private fun configureCompose(extension: CommonExtension<*, *, *, *, *, *>, target: Project) {
    val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    target.dependencies.add("implementation", libs.findBundle("compose").get())
    target.dependencies.add("debugImplementation", libs.findLibrary("compose-ui-tooling-core").get())
    target.plugins.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true
    target.tasks.withType<KotlinCompile> {
      kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
          "androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
          "androidx.compose.material3.ExperimentalMaterial3Api",
          "androidx.compose.foundation.ExperimentalFoundationApi",
          "androidx.compose.ui.ExperimentalComposeUiApi",
          "androidx.compose.animation.ExperimentalAnimationApi",
          "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        ).map { "-opt-in=$it" }
      }
    }
  }
}
