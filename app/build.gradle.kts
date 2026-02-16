plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.xenia.ticket"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.xenia.ticket"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("D:/XENIA/DOC/KEYS/KEYSTORE/MOBI/xeniamobi.jks")
            storePassword = "Xeniatech@2025"
            keyAlias = "xeniamobikey"
            keyPassword = "Xeniatech@2025"

            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildFeatures {
        aidl = true
        dataBinding
        viewBinding
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.activity.ktx)
    implementation(files("libs/printer-lib-2.2.4.aar"))
    implementation(files("libs/urovosdkLibs_New_v1.0.11.aar"))
    implementation(libs.androidx.activity.ktx.v1110)
    implementation(libs.androidx.activity.v1110)
    implementation(libs.flexbox)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.zxing.android.embedded)
    implementation (libs.glide)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation(libs.coil)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)

    //di
    implementation (libs.koin.android)

    //room
    implementation (libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.swiperefreshlayout)


    implementation(libs.androidx.lifecycle.process)


    implementation(libs.lottie)
}