plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.compose); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android { namespace = "com.lughzdan.app"; compileSdk = 36
 defaultConfig { minSdk = 26; applicationId = "com.lughzdan.app"; targetSdk = 36; versionCode = 1; versionName = "0.2.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildFeatures { compose = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":feature:games")); implementation(project(":domain")); implementation(project(":data")); implementation(project(":core:database")); implementation(project(":core:datastore")); implementation(libs.hilt.android); ksp(libs.hilt.compiler); implementation("androidx.activity:activity-compose:1.11.0"); implementation(platform("androidx.compose:compose-bom:2025.10.01")); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.foundation:foundation"); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
