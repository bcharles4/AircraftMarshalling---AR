plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.aircraftmarshalling"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aircraftmarshalling"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("androidx.gridlayout:gridlayout:1.0.0")

    // CameraX core
    implementation ("androidx.camera:camera-core:1.3.1")
    implementation ("androidx.camera:camera-camera2:1.3.1")

    // Lifecycle for binding
    implementation ("androidx.camera:camera-lifecycle:1.3.1")

    // PreviewView UI component
    implementation ("androidx.camera:camera-view:1.3.1")

    // If you want to use the base sdk
    implementation("com.google.mlkit:pose-detection:17.0.1-beta1")
    implementation("com.google.mlkit:pose-detection-accurate:17.0.1-beta1")

    implementation("com.android.volley:volley:1.2.1")

    implementation("com.google.android.filament:filament-android:1.57.1")
    implementation("com.google.android.filament:filament-utils-android:1.57.1")
    implementation("com.google.android.filament:gltfio-android:1.57.1")

}