plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firebase: el archivo google-services.json lo descarga el propietario desde Firebase Console.
// Si todavía no existe, la app compila igual y muestra "Firebase sin configurar".
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Dirección del servidor Zeni (HTTPS). Se inyecta al compilar y NO la escribe el usuario.
// Orden: -PzeniServerUrl=...  →  variable de entorno ZENI_SERVER_URL  →  vacío (sin configurar).
val zeniServerUrl: String = (
    (project.findProperty("zeniServerUrl") as String?)
        ?: System.getenv("ZENI_SERVER_URL")
        ?: ""
    ).trim().trimEnd('/').replace("\"", "")

android {
    namespace = "com.zeni.ia"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zeni.ia"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.3-online-login"
        // El J4 Core usa Android de 32 bits: incluimos ARM de 32 y 64 bits.
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        buildConfigField("String", "ZENI_SERVER_URL", "\"$zeniServerUrl\"")
    }

    // Clave de firma FIJA de pruebas (pública por diseño, como debug.keystore).
    // Google Sign-In necesita un SHA-1 estable; la clave debug de cada runner cambia en cada compilación.
    // Antes de publicar la app para otras personas hay que sustituirla por una clave privada.
    signingConfigs {
        create("zeniTest") {
            storeFile = file("zeni-test.keystore")
            storePassword = "android"
            keyAlias = "zenitest"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("zeniTest")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    // MediaPipe 0.10.27 incluye los cambios posteriores de compatibilidad ARMv7.
    implementation("com.google.mediapipe:tasks-genai:0.10.27")

    // Inicio de sesión: Firebase Authentication + "Iniciar sesión con Google" (Credential Manager).
    // Todo es Java/Kotlin puro (sin librerías nativas nuevas), así que no afecta a ARM de 32 bits.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
