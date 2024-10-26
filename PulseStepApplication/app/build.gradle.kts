plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.pulsestepapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pulsestepapplication"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

//       ndk {
//            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a","x86_64", "x86"))//,
//        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

dependencies {
    implementation ("androidx.fragment:fragment:1.5.5")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.0.0")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.imagepicker)
    implementation (libs.glide)
    implementation(libs.material.v150)
    implementation(libs.firebase.ui.storage)
    implementation(libs.circleimageview.v300)
    implementation (libs.android.gif.drawable)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.horizontalcalendar)
    implementation(libs.circleimageview)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.appcompat.v161)
    implementation(libs.activity.v180)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation (libs.firebase.storage)

    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.2.0")
}