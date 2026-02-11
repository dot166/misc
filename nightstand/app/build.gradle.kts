plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.dot166.nightstand"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "io.github.dot166.nightstand"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.jLib)
}