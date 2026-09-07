plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.sharedl"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sharedl"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.clear()
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            // Sign the Release APK so it can be installed on Android
            signingConfig = signingConfigs.getByName("debug")

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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val youtubedlAndroid = "0.18.1"

    implementation(
        "io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid"
    )

    implementation(
        "io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid"
    )
}
