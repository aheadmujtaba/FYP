plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fyp.realtimetextdetection"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.fyp.realtimetextdetection"
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
    buildFeatures{
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("androidx.multidex:multidex:2.0.1")

    // Text features
    implementation ("com.google.mlkit:text-recognition:16.0.1")
    implementation ("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation ("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation ("com.google.mlkit:text-recognition-japanese:16.0.1")
    implementation ("com.google.mlkit:text-recognition-korean:16.0.1")

    // ViewModel and LiveData
    implementation ("androidx.lifecycle:lifecycle-livedata:2.3.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.3.1")

    implementation ("androidx.appcompat:appcompat:1.2.0")
    implementation ("androidx.annotation:annotation:1.2.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.0.4")

    // CameraX
    implementation ("androidx.camera:camera-camera2:1.0.0-SNAPSHOT")
    implementation ("androidx.camera:camera-lifecycle:1.0.0-SNAPSHOT")
    implementation ("androidx.camera:camera-view:1.0.0-SNAPSHOT")

    implementation("com.google.mlkit:camera:16.0.0-beta3")
    implementation("com.google.mlkit:translate:17.0.2")

    // On Device Machine Learnings
    implementation ("com.google.android.odml:image:1.0.0-beta1")
}