@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
//import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform") version "1.8.10"
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
    jvm {
        jvmToolchain(15)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.0")
                // Decompose : Decompose
//                val decomposeVersion = "0.2.5"
//                implementation("com.arkivanov.decompose:decompose-jvm:$decomposeVersion")
//                implementation("com.arkivanov.decompose:extensions-compose-jetbrains-jvm:$decomposeVersion")
            }
        }
        val jvmTest by getting
    }
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
