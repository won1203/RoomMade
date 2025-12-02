import org.gradle.kotlin.dsl.androidTestImplementation
import org.gradle.kotlin.dsl.debugImplementation
import org.gradle.kotlin.dsl.implementation
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin 2.0+ required
}

private val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

private fun credentialProperty(name: String): String =
    (localProperties.getProperty(name)
        ?: (project.findProperty(name) as String?)
        ?: System.getenv(name)
        ?: "").trim()

fun String.escapeForBuildConfig(): String =
    this.replace("\\", "\\\\")
        .replace("\"", "\\\"")

val naverClientId: String = credentialProperty("NAVER_CLIENT_ID")
val naverClientSecret: String = credentialProperty("NAVER_CLIENT_SECRET")
val replicateApiKey: String = credentialProperty("REPLICATE_API_KEY")
val replicateControlnetVersion: String = credentialProperty("REPLICATE_CONTROLNET_VERSION")

android {
    namespace = "com.example.roommade"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.roommade"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "NAVER_CLIENT_ID",
            "\"${naverClientId.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "NAVER_CLIENT_SECRET",
            "\"${naverClientSecret.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "REPLICATE_API_KEY",
            "\"${replicateApiKey.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "REPLICATE_CONTROLNET_VERSION",
            "\"${replicateControlnetVersion.escapeForBuildConfig()}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Java 21 toolchain
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources {
        noCompress += listOf("tflite")
    }
}

kotlin {
    jvmToolchain(21) // Kotlin uses Java 21 toolchain
}

dependencies {
    // Core KTX helpers
    implementation("androidx.core:core-ktx:1.13.1")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Material Components (XML)
    implementation("com.google.android.material:material:1.12.0")
    // TFLite runtime: FULLY_CONNECTED v12 requires 2.17+
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    // WebView support + HTTP client
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
}

// Pin dependencies for legacy Android compatibility
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.inject" && requested.name == "guice") {
            useVersion("4.2.3")
            because("Guice 5.x requires Android O+ due to MethodHandle; pin to 4.2.3 for API 24.")
        }
        if (requested.group == "org.tensorflow" && requested.name == "tensorflow-lite") {
            useVersion("2.17.0")
            because("pin TensorFlow Lite runtime to a version that supports the model")
        }
    }
}
