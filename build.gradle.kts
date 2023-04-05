@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("org.jetbrains.compose") version "1.3.1"
}

group = "org.reactome.lit-ball"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.0")
    implementation ("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LitBall"
            packageVersion = "1.0.0"
        }
    }
}
