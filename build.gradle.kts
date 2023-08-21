import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("org.jetbrains.compose") version "1.4.3"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation("org.jetbrains.compose.material3:material3-desktop:1.4.3")
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.4.3")
    implementation ("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")
    implementation("com.squareup.okio:okio:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.slf4j:slf4j-log4j12:2.0.5")
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4")
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4:models")
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4:models-english")
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.4:models-english-kbp")
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

