import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.lineageos.generatebp)
}

group = "io.github.dot166"
version = providers.exec {
    commandLine("cat", "ver")
}.standardOutput.asText.get().trim()

android {
    namespace = "io.github.dot166.liblauncher3"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 31
    }

    buildTypes {
        release {
            //optimization {
            //    enable = false
            //}
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    api(libs.androidx.ui)
}

mavenPublishing {
    coordinates(group.toString(), name, version.toString())

    pom {
        name = "LibLauncher3"
        description = "library for my Launcher3 features"
        inceptionYear = "2026"
        url = "https://github.com/dot166/misc"
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT/")
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

configure<GenerateBpPluginExtension> {
    targetSdk.set(android.compileSdk!!)
    minSdk.set(android.defaultConfig.minSdk!!)
    versionCode.set(android.compileSdk!!)
    versionName.set(android.compileSdk!!.toString())
    availableInAOSP.set { module: Module ->
        when {
            module.group.startsWith("androidx") -> true
            module.group.startsWith("org.jetbrains") -> true
            module.group.startsWith("io.github.dot166") -> true
            module.group.startsWith("com.google") -> true
            else -> false
        }
    }
}
