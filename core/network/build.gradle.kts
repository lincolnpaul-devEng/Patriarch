import java.util.Properties

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties()
if (secretsPropertiesFile.exists()) {
    secretsProperties.load(secretsPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.campus.network.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "STREAM_API_KEY", "\"${secretsProperties.getProperty("STREAM_API_KEY", "")}\"")
        buildConfigField("String", "STREAM_SECRET_KEY", "\"${secretsProperties.getProperty("STREAM_SECRET_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${secretsProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secretsProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${secretsProperties.getProperty("OPENROUTER_API_KEY", "")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${secretsProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${secretsProperties.getProperty("CLOUDINARY_API_KEY", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${secretsProperties.getProperty("CLOUDINARY_API_SECRET", "")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${secretsProperties.getProperty("CLOUDINARY_UPLOAD_PRESET", "")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID_ANDROID", "\"${secretsProperties.getProperty("GOOGLE_CLIENT_ID_ANDROID", "")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID_WEB", "\"${secretsProperties.getProperty("GOOGLE_CLIENT_ID_WEB", "")}\"")
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
    implementation(project(":core:model"))
    implementation(project(":core:security"))

    // Stream Chat Client Engine
    api(libs.stream.chat.client)

    // Ktor WebSockets Engine
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)

    // Concurrency
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
