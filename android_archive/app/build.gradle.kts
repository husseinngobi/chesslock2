plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ngobi.chesslock"
    compileSdk = 34  // Compile SDK 34 for library compatibility

    defaultConfig {
        applicationId = "com.ngobi.chesslock"
        minSdk = 28  // Android 9 (API 28) - Client requirement
        targetSdk = 34  // Latest for Google Play compliance, but optimized for Android 9
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Support for vector drawables
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // For production, you should use proper keystore
            // For now, using debug keystore for client testing
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // Optimization for production
            isDebuggable = false
            isJniDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Modern Android components - now using version catalog
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.preference.ktx)
    
    // Coroutines for async operations - updated version
    implementation(libs.kotlinx.coroutines.android)
    
    // Animation and transitions - updated version
    implementation(libs.androidx.dynamicanimation)
    
    // Chess rules engine for proper game logic
    implementation("com.github.bhlangonijr:chesslib:1.3.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}