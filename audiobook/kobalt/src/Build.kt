/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.android.*
import com.beust.kobalt.plugin.java.*
import com.beust.kobalt.plugins
import com.beust.kobalt.*

val pl = plugins("com.beust:kobalt-android:0.16")

val rep = repos("https://jitpack.io")

val p = project {

    name = "MaterialAudiobookPlayer"
    group = "com.example"
    artifactId = name
    version = "0.1"

    android {
        compileSdkVersion = "23"
        applicationId = name
        buildToolsVersion = "23.0.2"
    }

    javaCompiler {
        args("-source" ,"1.7", "-target", "1.7")
    }

    dependencies {
        val supportVersion = "23.1.1"
        val okHttpVersion = "3.0.1"
        val daggerVersion = "2.0.2"
        val leakCanaryVersion = "1.4-beta1"
        val retroFitVersion = "2.0.0-beta3"

        compile("com.android.support:appcompat-v7:$supportVersion",
                "com.android.support:recyclerview-v7:$supportVersion",
                "com.android.support:support-v4:$supportVersion",

                "com.squareup.okhttp3:okhttp:$okHttpVersion",
                "com.squareup.okhttp3:okhttp-urlconnection:$okHttpVersion",
                // image loading
                "com.squareup.picasso:picasso:2.5.2",
                // material styled dialogs
                "com.afollestad.material-dialogs:core:0.8.5.3@aar",

                // fab
                "com.getbase:floatingactionbutton:1.10.1@aar",
                // crash reporting
                "ch.acra:acra:4.7.0",
                // dependency injection
                "com.google.dagger:dagger:$daggerVersion",


                //androidTestCompile "com.google.dagger:dagger:$daggerVersion" kapt "com.google.dagger:dagger-compiler:$daggerVersion" kaptAndroidTest "com.google.dagger:dagger-compiler:$daggerVersion" provided "javax.annotation:jsr250-api:1.0"
                // logging
                "com.jakewharton.timber:timber:4.1.0@aar",
                //testing
                //    testCompile "junit:junit:4.12"
                //  androidTestCompile("com.android.support.test.espresso:espresso-core:2.2.1") {
                //      exclude module: "support-annotations"
                //    }
                //  androidTestCompile("com.android.support.test:runner:0.4.1") {
                //       exclude module: "support-annotations"
                //    }
                //    androidTestCompile "com.jayway.android.robotium:robotium-solo:5.5.3"
                // rx extensions
                "io.reactivex:rxandroid:1.1.0@aar",
                "io.reactivex:rxjava:1.1.0",
                "com.jakewharton.rxbinding:rxbinding-kotlin:0.3.1-SNAPSHOT@aar",
                // detecting memory leaks
                //   debugCompile "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
                //    releaseCompile "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"
                // rest

                "com.squareup.retrofit2:retrofit:$retroFitVersion",

                "com.squareup.retrofit2:converter-gson:$retroFitVersion",
                "com.squareup.retrofit2:adapter-rxjava:$retroFitVersion",
                // kotlin
                // compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
                // player
                "com.github.AntennaPod:AntennaPod-AudioPlayer:v1.0.10@aar")
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
