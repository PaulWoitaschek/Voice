import com.beust.kobalt.*
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.java.*

val repos = repos()


val p = javaProject {

    name = "MaterialAudiobookPlayer"
    group = "com.example"
    artifactId = name
    version = "0.1"

    sourceDirectories {
        path("src/main/java")
        path("src/main/resources")
        path("src/main/res")
    }

    sourceDirectoriesTest {
        path("src/test/java")
        path("src/test/resources")
        path("src/test/res")
    }

    dependencies {
//        compile("com.beust:jcommander:1.48")
    }

    dependenciesTest {
//        compile("org.testng:testng:6.9.5")

    }

    assemble {
        jar {
        }
    }
}
