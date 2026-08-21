plugins {
    id("com.android.application")
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("${rootProject.projectDir}/local-repo")
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    implementation("top.canyie.pine:core:0.3.0")
    implementation("io.github.hexhacking:xdl:2.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
}

}
android {
    namespace = "dev.allofus.fusioncore"
    compileSdk = 36

    buildFeatures {
        prefab = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        applicationId = "dev.allofus.fusioncore"
        versionCode = 1
        versionName = "0.1"
        ndk {
            abiFilters.add("arm64-v8a")
            // abiFilters.add("armeabi-v7a")
        }
        proguardFile("proguard-unity.txt")
    }

    externalNativeBuild {
        cmake {
            path = File("./src/main/jni/CMakeLists.txt")
            version = "3.31.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf("*/armeabi-v7a/*.so", "*/arm64-v8a/*.so")
        }
    }

    lint {
        abortOnError = false
    }
}

