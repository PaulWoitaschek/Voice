import io.github.usefulness.KtlintGradleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure

class KtlintPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.apply("io.github.usefulness.ktlint-gradle-plugin")
    val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    target.extensions.configure<KtlintGradleExtension> {
      ktlintVersion.set(libs.findVersion("ktlint-core").get().requiredVersion)
    }
    target.dependencies.add("ktlintRuleSet", libs.findLibrary("ktlint-compose").get())
  }
}
