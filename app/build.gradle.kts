plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val versionCodeFromEnv = providers.environmentVariable("VERSION_CODE").orElse("1")
val versionNameFromEnv = providers.environmentVariable("VERSION_NAME").orElse("0.1.0-dev")

android {
    namespace = "com.mbush.notifymqtt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mbush.notifymqtt"
        minSdk = 35
        targetSdk = 36
        versionCode = versionCodeFromEnv.get().toInt()
        versionName = versionNameFromEnv.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.eclipse.paho)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
