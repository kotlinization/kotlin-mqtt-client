 plugins {
    kotlin("multiplatform") version libs.versions.kotlin
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

kotlin {
    jvm()
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(libs.kotlinxCoroutinesCore)
                implementation(libs.mppKtx)
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        getByName("jvmMain") { }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.moquette)
            }
        }
    }
}