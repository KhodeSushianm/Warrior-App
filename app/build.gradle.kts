plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.warrior.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.warrior.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-mvp"
    }

    buildTypes {
        debug {
            // Architecture Rule 11: destructive migration only in Debug, behind a flag.
            buildConfigField("boolean", "ALLOW_DESTRUCTIVE_MIGRATION", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ALLOW_DESTRUCTIVE_MIGRATION", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":data:local"))
    implementation(project(":domain:auth"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:workout"))
    implementation(project(":feature:history"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
}
