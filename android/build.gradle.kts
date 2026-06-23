plugins {
    id("com.android.application") version "8.7.0"
}

android {
    namespace = "de.blafoo.mandelbrot.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.blafoo.mandelbrot.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Core-Modul als lokale Maven-Abhängigkeit
    implementation("de.blafoo.mandelbrot:mandelbrot-core:1.0-SNAPSHOT")

    // Core Library Desugaring für Java 17 Records/Sealed auf älteren Android-Versionen
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // JSpecify Nullness-Annotationen
    implementation("org.jspecify:jspecify:1.0.0")
}
