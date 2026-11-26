plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "deez.nuts.egob"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "deez.nuts.egob"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.zxing.core)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.18.1")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    // optional - Test helpers
    testImplementation(libs.room.testing)
}