import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.20-RC"
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20-RC"
    id("com.github.gmazzo.buildconfig") version "5.4.0"
    id("dev.hydraulic.conveyor") version "1.10"
    id("com.github.ben-manes.versions") version "0.51.0"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks {
    named<Test>("test") {
        useTestNG()
        testLogging.showExceptions = true
        sourceSets {
            test {
                kotlin.srcDirs("src/commonTest/kotlin")
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-test-annotations-common:2.0.0-Beta4")
                    implementation("org.jetbrains.kotlin:kotlin-test-common:2.0.0-Beta4")
                    implementation("org.jetbrains.kotlin:kotlin-test:2.0.0-Beta4")
                }
            }
            main {
                kotlin.srcDirs("src/main/kotlin")
            }
        }
    }
    register<Copy>("changes") {
        from(layout.projectDirectory.file("CHANGES.txt"))
        rename("CHANGES.txt", "Changes.kt")
        into(layout.projectDirectory.file("src/main/kotlin/org/reactome/lit_ball/common"))
        filter { line ->
            when(line) {
                "## START" -> """
                    package org.reactome.lit_ball.common

                    object Changes {
                        val text = ""${'"'}
                """.trimIndent()
                "## END" -> """
                        ""${'"'}.trimIndent()
                    }
                """.trimIndent()
                else -> line
            }
        }
    }
    register("defaultScripts") {
        doLast {
            val scriptDir = layout.projectDirectory.dir("resources/scripts").asFile
            val scriptMap = emptyMap<String, String>().toMutableMap()
            scriptDir.walk().forEach {
                if (it.isFile) {
                    scriptMap[it.name] = it.readText()
                }
            }
            val srcFile = layout.projectDirectory.file("src/main/kotlin/org/reactome/lit_ball/util/DefaultScriptsData.kt").asFile
            srcFile.writeText("""
                package org.reactome.lit_ball.util

                    object DefaultScriptsData {
                        val scriptMap = mapOf<String, String>(
                    """.trimIndent())
            scriptMap.toSortedMap().forEach { k, v ->
                val text = v.replace("\"\"\"", "longstringdelimiterreplacement")
                srcFile.appendText("\"$k\" to \"\"\"${text}\"\"\",\n")
            }

            srcFile.appendText("""
                ) }
            """.trimIndent())
        }
    }
    compileKotlin {
        dependsOn("changes")
        dependsOn("defaultScripts")
    }
}

kotlin {
    jvmToolchain(20)
//    project.sourceSets.create("main")
    project.sourceSets.create("commonTest")
    sourceSets {
        val main: KotlinSourceSet by getting {
            kotlin.srcDirs("src/main/kotlin")
            resources.srcDirs("resources")
            dependencies {
                //
            }
        }
        val commonTest: KotlinSourceSet by getting {
            kotlin.srcDirs("src/commonTest/kotlin")
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation ("org.jetbrains.kotlin:kotlin-test-annotations-common:2.0.0-Beta4")
                implementation ("org.jetbrains.kotlin:kotlin-test-common:2.0.0-Beta4")
                implementation ("org.jetbrains.kotlin:kotlin-test:2.0.0-Beta4")
            }
        }
    }}

dependencies {
    implementation("org.jetbrains.compose.material3:material3-desktop:1.6.11")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.12")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation ("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")
    implementation("dev.dirs:directories:26")
    implementation("org.testng:testng:7.10.2")
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-kernel:4.7.0")
    implementation("org.jetbrains.lets-plot:lets-plot-common:4.3.3")
    implementation("org.jetbrains.lets-plot:platf-awt:4.3.1")
    implementation("org.jetbrains.lets-plot:lets-plot-compose:1.0.3")
    implementation("org.apache.commons:commons-jexl3:3.4.0")
    implementation("org.apache.jena:jena-arq:5.0.0-rc1")

    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}


buildConfig {
    packageName("org.reactome.lit_ball")  // forces the package. Defaults to '${project.group}'
    buildConfigField("String", "APP_NAME", "\"LitBall\"")
    buildConfigField("String", "APP_VERSION", provider { "\"2405\"" })
}

configurations.all {
    attributes {
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        version = "2405"
        group = "org.reactome"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LitBall"
            packageVersion = "23.08.0"
        }
    }
}
