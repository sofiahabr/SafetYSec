plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"  // Add this
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX and Material Design
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    implementation(libs.androidx.media3.exoplayer)
    ksp("com.google.dagger:hilt-compiler:2.48.1")

    // Jetpack Compose - Use BOM for version alignment
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Other essential Compose libraries
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

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

    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // CameraX for video recording
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-video:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

// ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

// Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

// ============================================
// PHASE 6: Alert System Dependencies
// ============================================

// CameraX for video recording (30-second alert videos)
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-video:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.camera:camera-extensions:1.3.0")

// Firebase Cloud Messaging for push notifications
    implementation("com.google.firebase:firebase-messaging:23.3.1")

// Coroutines for async operations (may already be present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

// Firebase Storage (should already be present)
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")

// Firebase Firestore (should already be present)
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")

// Firebase Auth (should already be present)
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")

// Lifecycle components (should already be present)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
}
