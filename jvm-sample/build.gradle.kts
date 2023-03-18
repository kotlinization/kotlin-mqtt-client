plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.3.1"
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    jcenter()
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