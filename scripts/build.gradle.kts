plugins {
  alias(libs.plugins.kotlin.jvm)
  application
}

application {
  mainClass.set("voice.scripts.ScriptKt")
}

dependencies {
  implementation(libs.clikt)
}
