plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.dot166.felicatestapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.dot166.felicatestapp"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = versionCode.toString()
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}