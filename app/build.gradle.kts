plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.adarsh.glass"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adarsh.glass"
        minSdk = 31
        targetSdk = 35
        // CI passes the GitHub run number so every build has a unique, increasing version code
        // (installs cleanly as an update). Falls back to 1 for local builds.
        versionCode = (project.findProperty("glassVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("glassVersionName") as String?) ?: "0.1.0"
    }

    // Fixed debug keystore committed in the repo so every build is signed with the
    // same certificate — new APKs install as an update, no uninstall needed.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            // Keep release installable alongside debug by signing it with the same
            // fixed debug key for now (personal sideload, no Play Store).
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation.core)

    debugImplementation(libs.androidx.ui.tooling)
}
