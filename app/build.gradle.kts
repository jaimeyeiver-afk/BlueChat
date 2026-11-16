import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // Lee app/google-services.json (proyecto Firebase "zeni-ia", paquete com.zeni.ia).
    id("com.google.gms.google-services")
}

android {
    namespace = "com.zeni.ia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zeni.ia"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "2.1-internet-appcheck"
        // Sin librerías nativas: compatible con ARM de 32 y 64 bits.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/INDEX.LIST",
            "META-INF/DEPENDENCIES",
            "META-INF/io.netty.versions.properties"
        )
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Firebase AI Logic y App Check: Play Integrity en release; proveedor debug solo en debug.
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
}
