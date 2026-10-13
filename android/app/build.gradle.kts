plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.encorevpn.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.encorevpn.app"
        minSdk = 24; targetSdk = 34; versionCode = 1; versionName = "1.0"
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { useLegacyPackaging = true } }   // бинарь ядра должен распаковаться на диск
    buildFeatures { compose = true }                       // UI на Jetpack Compose
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }   // пара к Kotlin 1.9.24
}
dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
