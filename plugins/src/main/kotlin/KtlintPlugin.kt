import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class KtlintPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    target.pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
      val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
      target.dependencies.add("ktlintRuleset", libs.findLibrary("ktlint-compose").get())
      target.extensions.configure<KtlintExtension> {
        version.set(libs.findVersion("ktlint-core").get().requiredVersion)
      }
    }
  }
}
