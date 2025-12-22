plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("kotlin-parcelize")

}

android {
    namespace = "com.rolando.locallink"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.rolando.locallink"
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
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3) // Uses 1.4.0 from your catalog

    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.runtime.saveable)

    // Navigation & animations
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")

    // Coil for images
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase (Keeping BOM here is standard, but you can remove if you want)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Data
    implementation(libs.androidx.datastore.core)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // LiveData
    implementation("androidx.compose.runtime:runtime-livedata:1.7.0")


    // 1. The BOM controls the version for ALL modules (Use 3.0.0 or latest)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))

    // 2. Add modules WITHOUT version numbers (BOM handles it)
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Serialization (Keep the version here as it's a different library)
    implementation("io.ktor:ktor-client-android:3.0.0")

    // 👇 KTOR ENGINE (Must match Supabase 3.2.6 requirements)
    implementation("io.ktor:ktor-client-android:3.0.1")
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.room.common.jvm)



    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}