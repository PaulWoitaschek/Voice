plugins {
  id("com.gradle.enterprise") version "3.0"
}

gradleEnterprise {
  buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
  }
}


include(":app")
include(":core")
include(":common")
include(":data")
include(":covercolorextractor")
include(":crashreporting")
