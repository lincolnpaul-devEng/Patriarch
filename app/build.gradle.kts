import java.util.Properties

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties()
if (secretsPropertiesFile.exists()) {
    secretsProperties.load(secretsPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.campus.network"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.campus.network"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "STREAM_API_KEY", "\"${secretsProperties.getProperty("STREAM_API_KEY", "")}\"")
        buildConfigField("String", "STREAM_SECRET_KEY", "\"${secretsProperties.getProperty("STREAM_SECRET_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${secretsProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secretsProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${secretsProperties.getProperty("OPENROUTER_API_KEY", "")}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${secretsProperties.getProperty("GOOGLE_MAPS_API_KEY", "")}\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(libs.stream.chat.client)

    implementation(project(":feature:map"))
    implementation(project(":feature:channels"))
    implementation(project(":feature:messages"))
    implementation(project(":feature:hub"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.emoji2.bundled)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
