import com.autonomousapps.DependencyAnalysisSubExtension
import com.autonomousapps.extension.Issue
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure

const val DEPENDENCY_ANALYSIS_PLUGIN = "com.autonomousapps.dependency-analysis"

// The convention plugins add these to every module, used or not, so a module has no declaration to fix.
fun Project.excludeFromDependencyAnalysis(exclusions: Issue.() -> Unit) {
  pluginManager.withPlugin(DEPENDENCY_ANALYSIS_PLUGIN) {
    extensions.configure<DependencyAnalysisSubExtension> {
      issues {
        onAny(exclusions)
      }
    }
  }
}

fun Project.excludeFromDependencyAnalysis(vararg dependencies: Provider<MinimalExternalModuleDependency>) {
  excludeFromDependencyAnalysis { exclude(*dependencies) }
}

fun Project.excludeFromDependencyAnalysis(bundle: Provider<ExternalModuleDependencyBundle>) {
  excludeFromDependencyAnalysis { exclude(*bundle.get().map { it.module.toString() }.toTypedArray()) }
}
