plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.compose) }
android { namespace = "com.lughzdan.designsystem"; compileSdk = 36
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildFeatures { compose = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { ; implementation(platform("androidx.compose:compose-bom:2025.10.01")); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.foundation:foundation"); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
