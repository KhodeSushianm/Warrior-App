plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.warrior.data.local"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug {
            // Architecture Rule 11: destructive fallback is Debug-only, behind a flag.
            buildConfigField("boolean", "ALLOW_DESTRUCTIVE_MIGRATION", "true")
        }
        release {
            buildConfigField("boolean", "ALLOW_DESTRUCTIVE_MIGRATION", "false")
        }
    }
    sourceSets {
        // Frozen schema JSONs double as test assets for MigrationTestHelper (Phase 10).
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.maxHeapSize = "384m" }
        }
    }
}

ksp {
    // Architecture Rule: schema JSON files live in VCS so future migrations are testable.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))
    implementation(project(":domain:auth"))
    implementation(project(":domain:training"))
    implementation(project(":domain:progress"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
