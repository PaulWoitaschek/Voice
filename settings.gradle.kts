plugins {
  id("com.gradle.enterprise") version "3.0"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(":app")
include(":core")
include(":common")
include(":data")
include(":covercolorextractor")
include(":crashreporting")
include(":playback")
include(":prefs")
include(":ffmpeg")
