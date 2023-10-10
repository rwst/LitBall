import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("org.jetbrains.compose") version "1.5.0"
    id("com.github.gmazzo.buildconfig") version "4.1.2"
    id("dev.hydraulic.conveyor") version "1.5"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
                // ...
            }
        }
        val commonTest: KotlinSourceSet by getting {
            kotlin.srcDirs("src/commonTest/kotlin")
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation ("org.jetbrains.kotlin:kotlin-test-annotations-common:1.9.0")
                implementation ("org.jetbrains.kotlin:kotlin-test-common:1.9.0")
                implementation ("org.jetbrains.kotlin:kotlin-test:1.9.0")
            }
        }
    }}

dependencies {
    implementation("org.jetbrains.compose.material3:material3-desktop:1.5.0")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("com.squareup.okio:okio:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("io.github.oshai:kotlin-logging-jvm:5.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("dev.dirs:directories:26")
    implementation("org.testng:testng:7.7.0")
    implementation("org.jetbrains.lets-plot:lets-plot-batik:4.0.1")
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.4.3")
    implementation("org.apache.commons:commons-jexl3:3.2.1")

    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}

tasks {
    named<Test>("test") {
        useTestNG()
        testLogging.showExceptions = true
        sourceSets {
            test {
                kotlin.srcDirs("src/commonTest/kotlin")
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-test-annotations-common:1.9.0")
                    implementation("org.jetbrains.kotlin:kotlin-test-common:1.9.0")
                    implementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
                }
            }
            main {
                kotlin.srcDirs("src/main/kotlin")
            }
        }
    }
}

buildConfig {
    packageName("org.reactome.lit_ball")  // forces the package. Defaults to '${project.group}'
    buildConfigField("String", "APP_NAME", "\"LitBall\"")
    buildConfigField("String", "APP_VERSION", provider { "\"2320\"" })
}

configurations.all {
    attributes {
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        version = "2320"
        group = "org.reactome"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LitBall"
            packageVersion = "23.08.0"
        }
    }
}
