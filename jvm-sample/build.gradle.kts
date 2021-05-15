import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "0.4.0-build190"
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    jcenter()
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