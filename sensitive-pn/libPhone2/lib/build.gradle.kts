plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.aconfig)
}

group = "io.github.dot166"
version = providers.exec {
    commandLine("cat", "ver")
}.standardOutput.asText.get().trim()

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
    api(libs.annotation)
}

aconfig {
    aconfigFiles = mutableListOf("aconfig/libphone2.aconfig")
    isAOSP = false
}

mavenPublishing {
    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name = "libPhone2"
        description = "reimplementation of LineageOS libPhone"
        inceptionYear = "2026"
        url = "https://github.com/dot166/misc"
        licenses {
            license {
                name.set("MIT License")
                url.set("https://choosealicense.com/licenses/mit/")
            }
        }
        developers {
            developer {
                id = "dot166"
                name = "._______166"
                url = "https://dot166.github.io"
            }
        }
        scm {
            url = "https://github.com/dot166/misc"
            connection = "scm:git:git://github.com/dot166/misc.git"
            developerConnection = "scm:git:ssh://git@github.com/dot166/misc.git"
        }
    }
}