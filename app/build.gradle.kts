plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.ticket"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.example.ticket"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        aidl = true
        dataBinding
        viewBinding
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
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
    implementation("androidx.activity:activity-ktx:1.11.0")
    testImplementation("junit:junit:4.13.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    implementation ("com.squareup.retrofit2:retrofit:3.0.0")
    implementation ("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")


    //di
    implementation ("io.insert-koin:koin-android:4.1.1")

    //room
    implementation ("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")
}