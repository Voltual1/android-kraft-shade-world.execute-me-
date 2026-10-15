import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    alias(libs.plugins.compose.compiler) 
    alias(libs.plugins.kotlin.serialization)    
}

android {
    namespace = "me.voltual.asp"
    
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),                
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    lint {
        abortOnError = false
    }
}

dependencies {
    if (gradleLocalProperties(rootDir, providers).getProperty("library.build_with_submodule") == "true") {
        implementation(project(":kraft-shade-compose"))
    } else {
        implementation(libs.kraftshade.compose)
    }
    if (gradleLocalProperties(rootDir, providers).getProperty("library.build_with_submodule") == "true") {
        implementation(project(":kraft-shade-media3"))
    } else {
        implementation(libs.kraftshade.media3)
    }

    // Jetpack
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.jetBrains.immutableCollections)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.transformer)

    // Adding GPUImage as a reference for development.
    implementation(libs.image.processing.gpuImage)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.io)
    
    implementation(libs.coil.compose)
}
