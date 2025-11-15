plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.safetysec"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.safetysec"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    kapt {
        correctErrorTypes = true
        arguments {
            arg("dagger.fastInit", "enabled")
            arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
        }
    }
}

dependencies {
    // AndroidX and Material Design
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")

    // Jetpack Compose - Use BOM for version alignment
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core Activity
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Firebase Bill of Materials (BOM) - manages all Firebase SDK versions
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firebase Firestore (Real-time Database)
    implementation("com.google.firebase:firebase-firestore")

    // Firebase Cloud Storage (for video uploads)
    implementation("com.google.firebase:firebase-storage")

    // Firebase Realtime Database (alternative to Firestore)
    implementation("com.google.firebase:firebase-database")

    // Firebase Cloud Messaging (for push notifications)
    implementation("com.google.firebase:firebase-messaging")

    // Google Play Services (required for location services)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Kotlin Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}