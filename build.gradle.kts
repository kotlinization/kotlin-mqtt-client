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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxCoroutinesCore)
                implementation(libs.mppKtx)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting { }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}