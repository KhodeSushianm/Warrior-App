import java.util.Properties

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
        versionCode = 4
        versionName = "1.1.1"
    }

    signingConfigs {
        // RC signing (Phase 10): self-signed key committed for reproducible
        // sideload builds — see keystore.properties for the security note.
        create("release") {
            val keystoreProperties = Properties().apply {
                val file = rootProject.file("keystore.properties")
                if (file.exists()) file.inputStream().use { load(it) }
            }
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile", "keystore/warrior-release.jks"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        // Architecture Rule 11 (Phase 10): the ALLOW_DESTRUCTIVE_MIGRATION flag
        // lives in :data:local's own BuildConfig — debug-only there; release
        // builds fail loudly on a missing migration instead of wiping data.
        release {
            // Sandbox note (Phase 15 revision): R8 over the full graph needs
            // ~700MB+ heap which the 1GiB CI sandbox cannot give; releases built
            // via scripts/build-release.sh ship un-minified for now. The
            // standalone-R8 path stays in the script (WARRIOR_MINIFY=1) for
            // machines with >=4GB RAM / future CI (Phase 20).
            isMinifyEnabled = providers.environmentVariable("WARRIOR_MINIFY").orNull == "1"
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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

tasks.withType<JavaCompile>().configureEach {
    // 1GiB sandbox: Hilt-generated sources spike metaspace beyond the cgroup
    // limit when javac runs in-process; fork it into its own small JVM.
    options.isFork = true
    options.forkOptions.memoryMaximumSize = "320m"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":data:local"))
    implementation(project(":domain:auth"))
    implementation(project(":domain:progress"))
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
