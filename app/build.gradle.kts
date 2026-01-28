plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.addAll("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

android {
    namespace = "ch.opum.tricktrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "ch.opum.tricktrack"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents.onVariants { variant ->
    variant.outputs.forEach { output ->
        val outputImpl = output as com.android.build.api.variant.impl.VariantOutputImpl
        outputImpl.outputFileName.set("TrickTrack-v${android.defaultConfig.versionName}.apk")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Removed: implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Location
    implementation(libs.google.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // Gson
    implementation(libs.google.code.gson)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    androidTestImplementation(libs.androidx.work.testing)

    // DocumentFile
    implementation(libs.androidx.documentfile)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}