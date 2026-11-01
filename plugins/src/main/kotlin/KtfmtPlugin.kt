import com.diffplug.gradle.spotless.BaseKotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure

class KtfmtPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.apply("com.diffplug.spotless")
    val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val ktfmtVersion = libs.findVersion("ktfmt").get().requiredVersion
    target.extensions.configure<SpotlessExtension> {
      kotlin {
        target("src/**/*.kt")
        voiceKtfmt(ktfmtVersion)
      }
      kotlinGradle {
        target("*.kts")
        voiceKtfmt(ktfmtVersion)
      }
    }
  }

  private fun BaseKotlinExtension.voiceKtfmt(version: String) {
    ktfmt(version).googleStyle().configure { it.setMaxWidth(140) }
  }
}
