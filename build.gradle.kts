plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // Se aplica en :app solo si existe app/google-services.json (ver app/build.gradle.kts).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
