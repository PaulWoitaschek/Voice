import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.project

class FeaturePlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.apply("voice.library")
    target.pluginManager.apply("voice.compose")
    target.dependencies.run {
      listOf(":core:common", ":core:strings", ":core:ui", ":navigation").forEach { path ->
        add("implementation", project(path))
      }
    }
  }
}
