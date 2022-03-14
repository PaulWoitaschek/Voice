import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.baseSetup() {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = VoiceVersions.javaCompileVersion.toString()
      freeCompilerArgs = freeCompilerArgs + listOf(
        "-progressive",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlinx.coroutines.FlowPreview",
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-Xopt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
        "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
      )
    }
  }
}
