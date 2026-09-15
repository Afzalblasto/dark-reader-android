plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.darkreader.app"
    //noinspection EditedTargetSdkVersion
    compileSdk = 37

    defaultConfig {
        applicationId = "com.darkreader.app"
        minSdk = 26
        //noinspection EditedTargetSdkVersion
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "META-INF/versions/**"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // DocumentFile
    implementation("androidx.documentfile:documentfile:1.0.1")

    // PDF manipulation (Apache PDFBox Android port)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Gson for JSON export/import
    implementation("com.google.code.gson:gson:2.11.0")

    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.8.2")

    // Activity KTX
    implementation("androidx.activity:activity-ktx:1.9.1")

    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Security (encrypted shared prefs for Safe PIN)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
