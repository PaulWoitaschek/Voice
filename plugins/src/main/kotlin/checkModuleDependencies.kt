import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

// enforces the dependency rules of .agents/skills/architecture/SKILL.md
fun checkModuleDependencies(target: Project) {
  val from = target.path
  if (from == ":app") return
  target.configurations.configureEach {
    val isTest = name.contains("test", ignoreCase = true)
    dependencies.withType(ProjectDependency::class.java).configureEach {
      val to = path
      val violation = moduleDependencyViolation(from = from, to = to, isTest = isTest)
      if (violation != null) {
        throw GradleException("$from must not depend on $to. $violation")
      }
    }
  }
}

private fun moduleDependencyViolation(
  from: String,
  to: String,
  isTest: Boolean,
): String? {
  val toFeature = to.feature()
  val fromFeature = from.feature()
  return when {
    to == from -> null
    toFeature != null && fromFeature == null -> "Only :app depends on features."
    toFeature != null && !(toFeature == fromFeature && to.isApi()) ->
      "Features don't depend on other features, only on their own :api module."
    !isTest && to.isImplementation() -> "Depend on the :api module instead, :app picks the implementation."
    else -> null
  }
}

private fun String.segments(): List<String> = removePrefix(":").split(":")

private fun String.feature(): String? {
  val segments = segments()
  return if (segments.size >= 2 && segments.first() == "features") segments[1] else null
}

private fun String.isApi(): Boolean = segments().last() == "api"

// every nested module is part of an :api and implementations split, like :core:data:impl
private fun String.isImplementation(): Boolean = segments().size >= 3 && !isApi()
