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

        // Pluggable SafeTrade escrow payment socket (leave blank until a free/prod API is connected)
        buildConfigField("String", "PAYMENT_API_BASE_URL", "\"${secretsProperties.getProperty("PAYMENT_API_BASE_URL", "")}\"")
        buildConfigField("String", "PAYMENT_API_KEY", "\"${secretsProperties.getProperty("PAYMENT_API_KEY", "")}\"")
        buildConfigField("String", "PAYMENT_API_SECRET", "\"${secretsProperties.getProperty("PAYMENT_API_SECRET", "")}\"")
        buildConfigField("String", "PAYMENT_PROVIDER_ID", "\"${secretsProperties.getProperty("PAYMENT_PROVIDER_ID", "")}\"")
        buildConfigField(
            "String",
            "PAYMENT_PROVIDER_DISPLAY_NAME",
            "\"${secretsProperties.getProperty("PAYMENT_PROVIDER_DISPLAY_NAME", "Campus SafePay")}\""
        )
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

    // Stream Video / Audio (CallMediaProvider)
    api(libs.stream.video.core)

    // Ktor WebSockets Engine
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)

    // Concurrency
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    testImplementation(libs.kotlinx.coroutines.core)
}
