plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // For Room database
    id("com.google.gms.google-services") // ADD THIS LINE
}

android {
    namespace = "com.example.recipe"
    compileSdk = 35

    defaultConfig {
        applicationId = "ke.nucho.recipe"
        minSdk = 24
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android dependencies (using version catalog)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Animation (using BOM version)
    implementation("androidx.compose.animation:animation")

    // Material Design 3 - REQUIRED for Theme.Material3.DayNight
    implementation("com.google.android.material:material:1.11.0")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room database
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    kapt("androidx.room:room-compiler:2.5.0")

    // ==========================================
    // FIREBASE DEPENDENCIES - ADD THESE
    // ==========================================
    // Firebase BoM (Bill of Materials) - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firestore for caching recipes
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase Analytics (optional but recommended)
    implementation("com.google.firebase:firebase-analytics-ktx")
    // Firebase Remote Config - for dynamic API key
    implementation("com.google.firebase:firebase-config-ktx")
    // ==========================================
    // ADD THIS - Gson for JSON parsing
    // ==========================================
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.google.android.material:material:1.11.0")
    // ==========================================
// ADMOB DEPENDENCIES - ADD THIS
// ==========================================
// Google AdMob SDK
    implementation("com.google.android.gms:play-services-ads:23.0.0")

        // Lifecycle components
        implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
        implementation ("androidx.lifecycle:lifecycle-process:2.6.2")

        // Google Ads
        implementation ("com.google.android.gms:play-services-ads:22.6.0")


}