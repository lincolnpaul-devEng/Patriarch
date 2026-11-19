import java.util.Properties

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties()
if (secretsPropertiesFile.exists()) {
    secretsProperties.load(secretsPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.campus.network.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "STREAM_API_KEY", "\"${secretsProperties.getProperty("STREAM_API_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${secretsProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secretsProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${secretsProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID_ANDROID", "\"${secretsProperties.getProperty("GOOGLE_CLIENT_ID_ANDROID", "")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID_WEB", "\"${secretsProperties.getProperty("GOOGLE_CLIENT_ID_WEB", "")}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${secretsProperties.getProperty("OPENROUTER_API_KEY", "")}\"")
        buildConfigField("boolean", "MPESA_ENABLED", "${secretsProperties.getProperty("MPESA_ENABLED", "false") == "true"}")

    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    constraints {
        api("org.jetbrains.kotlin:kotlin-stdlib:1.9.23") {
            because("Supabase BOM pulls in incompatible Kotlin stdlib version")
        }
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") {
            because("Supabase BOM pulls in incompatible coroutines version")
        }
        api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") {
            because("Supabase BOM pulls in incompatible coroutines version")
        }
    }
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
        }
    }
    implementation(project(":core:model"))
    implementation(project(":core:security"))

    // Stream Chat Client Engine
    api(libs.stream.chat.client)

    // Stream Video / Audio (CallMediaProvider)
    api(libs.stream.video.core)

    // Ktor WebSockets Engine (kept during migration; payment/Cloudinary/OpenRouter still use it)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)

    // Concurrency
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Supabase Kotlin SDK (migrating from custom Ktor REST — side-by-side during transition)
    api(platform(libs.supabase.bom))
    api(libs.supabase.postgrest)
    api(libs.supabase.gotrue)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    testImplementation(libs.kotlinx.coroutines.core)
    // Robolectric for the Android-backed SupabaseSessionStore (EncryptedSharedPreferences)
    testImplementation("org.robolectric:robolectric:4.13")
}
