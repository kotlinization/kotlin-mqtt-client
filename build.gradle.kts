plugins {
    kotlin("multiplatform") version "1.4.30"
}

val coroutinesVersion = "1.4.2"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("com.github.kotlinizer.mpp-ktx:mpp-ktx:v0.1.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.mockk:mockk:1.10.0")
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