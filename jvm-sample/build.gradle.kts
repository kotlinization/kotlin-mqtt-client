plugins {
    kotlin("jvm")
    alias(libs.plugins.compose)
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":"))
}

compose.desktop {
    application {
        mainClass = "com.github.kotlinizer.mqtt.jvm.sample.MainKt"
    }
}