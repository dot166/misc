plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.aconfig)
}

group = "io.github.dot166"
version = 1

android {
    namespace = "io.github.dot166.libphone2"
    compileSdk = 36

    defaultConfig {
        minSdk = 31

        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets {
        named("main") {
            kotlin.directories.add("compat/src/main/kotlin")
        }
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
    api(libs.libphonenumber)
    implementation(libs.annotation)
}

aconfig {
    aconfigFiles = mutableListOf("aconfig/libphone2.aconfig")
    isAOSP = false
}

