import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.lingoFlix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lingoFlix"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { props.load(it) }
            }

            storeFile = file(props.getProperty("RELEASE_STORE_FILE") ?: "release.keystore")
            storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }
}

// Name the output APK (AGP 9 removed the legacy applicationVariants API)
base {
    archivesName.set("LingoFlix_v${android.defaultConfig.versionName}")
}

// --- Robust Code Quality Build Task ---
tasks.register("checkCodeQuality") {
    group = "verification"
    description = "Enforces strict project standards: 300-line rule, error handling, and logging."
    
    doLast {
        val srcDir = file("src/main/java/com/example/lingoFlix")
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        srcDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            val fileName = file.name
            
            // 1. Line Count Rule (Hard Error)
            if (lines.size > 350) {
                errors.add("$fileName: Too long (${lines.size} lines). Must be under 350.")
            }
            
            // 2. Logging Rule (Warning)
            if (lines.size > 50 && !lines.any { it.contains("LingoLog") } && !fileName.contains("ViewModel")) {
                warnings.add("$fileName: Should use LingoLog for consistency.")
            }

            // 3. Error Handling (Warning)
            if (lines.size > 100 && !lines.any { it.contains("try {") }) {
                warnings.add("$fileName: No try-catch blocks found in a large file.")
            }
            
            // 4. Hardcoded Strings (Warning - simple check)
            if (lines.any { it.contains("Toast.makeText") && it.contains("\"") && !it.contains("R.string") }) {
                warnings.add("$fileName: Found hardcoded string in Toast. Use resources.")
            }
        }
        
        println("\n--- Code Quality Report ---")
        warnings.forEach { println("[WARN] $it") }
        if (errors.isNotEmpty()) {
            errors.forEach { System.err.println("[FAIL] $it") }
            throw GradleException("Code quality check failed with ${errors.size} errors.")
        }
        println("Code Quality passed with ${warnings.size} warnings.")
    }
}

tasks.named("preBuild") {
    dependsOn("checkCodeQuality")
}
// --------------------------------------

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)

    // Networking (LingoFlix backend)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // YouTube IFrame player (subtitles/quiz overlay on top of YouTube)
    implementation(libs.youtube.player)

    // ML Kit & Offline features
    implementation(libs.google.mlkit.translate)
    implementation(libs.google.mlkit.language.id)

    // AI & Networking
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(libs.androidx.security.crypto)

    // UI & Animations
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")

    // Room Database
    val room_version = "2.8.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
}
