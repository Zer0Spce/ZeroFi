plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zer0spce.zerofi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zer0spce.zerofi"
        minSdk = 23
        targetSdk = 37
        versionCode = 8
        versionName = "1.2.1"
    }

    val updateStorePath = System.getenv("ZEROFI_SIGNING_STORE_FILE")
    val updateStoreFile = updateStorePath?.let(::file)
    val hasUpdateSigningKey = updateStoreFile?.exists() == true

    if (hasUpdateSigningKey) {
        signingConfigs {
            create("zerofiUpdate") {
                storeFile = updateStoreFile
                storePassword = System.getenv("ZEROFI_SIGNING_STORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("ZEROFI_SIGNING_KEY_ALIAS") ?: "zerofi"
                keyPassword = System.getenv("ZEROFI_SIGNING_KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasUpdateSigningKey) {
                signingConfig = signingConfigs.getByName("zerofiUpdate")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.zxing:core:3.5.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
