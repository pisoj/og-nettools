plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
}

android {
    namespace = "xyz.pisoj.holo1"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.pisoj.holo1"
        minSdk = 11
        targetSdk = 34
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    //noinspection GradleCompatible,GradleCompatible
    implementation("com.android.support:support-fragment:25.0.0")
    //noinspection GradleCompatible
    implementation("com.android.support:recyclerview-v7:25.0.0")
    //noinspection GradleCompatible
    implementation("com.android.support:support-core-ui:25.0.0")
    //noinspection GradleDependency
    implementation("com.android.support.constraint:constraint-layout:1.1.3")
    testImplementation("junit:junit:4.13.2")
}