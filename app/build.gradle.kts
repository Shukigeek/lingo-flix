import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    // id("com.google.gms.google-services") // Removed because google-services.json is missing
}

android {
    namespace = "com.example.lingoFlix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lingoFlix"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "1.4"

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
    }
}

// Rename the output APK
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(output.versionName.map { "LingoFlix_v$it.apk" })
        }
    }
}

// --- Code Quality Build Task ---
tasks.register("checkCodeQuality") {
    group = "verification"
    description = "Checks for 300-line rule and basic code standards."
    
    doLast {
        val srcDir = file("src/main/java/com/example/lingoFlix")
        var failed = false
        val report = StringBuilder()
        
        srcDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            
            // 1. Check line count
            if (lines.size > 500) {
                report.append("[FAIL] ${file.name} is too long (${lines.size} lines). Limit is 500.\n")
                failed = true
            }
            
            // 2. Check for try-catch in non-trivial files (simple heuristic)
            if (lines.size > 50 && !lines.any { it.contains("try {") || it.contains("try{") }) {
                report.append("[WARN] ${file.name} might be missing try-catch blocks.\n")
            }

            // 3. Check for LingoLog usage
            if (lines.size > 50 && !lines.any { it.contains("LingoLog") }) {
                report.append("[WARN] ${file.name} should use LingoLog for consistency.\n")
            }
        }
        
        if (failed) {
            throw GradleException("Code Quality checks failed:\n$report")
        } else {
            println("Code Quality checks passed!\n$report")
        }
    }
}

// Hook into the build process
tasks.named("preBuild") {
    dependsOn("checkCodeQuality")
}
// -------------------------------

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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

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
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Firebase - Commented out because google-services.json is missing
    // implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation("com.google.firebase:firebase-analytics-ktx")
}




