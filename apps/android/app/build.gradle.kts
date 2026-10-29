plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.compose); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android { namespace = "com.lughzdan.app"; compileSdk = 36
 defaultConfig { minSdk = 26; applicationId = "com.lughzdan.app"; targetSdk = 36; versionCode = 3; versionName = "0.4.0-dev"; buildConfigField("String", "CONTENT_API_URL", "\"" + providers.gradleProperty("contentApiUrl").orElse("").get().also { require(it.isEmpty() || it.matches(Regex("https://[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9/_-]*"))) } + "\"") }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildFeatures { compose = true; buildConfig = true }
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":feature:games")); implementation(project(":domain")); implementation(project(":data")); implementation(project(":core:database")); implementation(project(":core:datastore")); implementation(libs.hilt.android); ksp(libs.hilt.compiler); implementation("androidx.activity:activity-compose:1.11.0"); implementation(platform("androidx.compose:compose-bom:2025.10.01")); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.foundation:foundation"); implementation(libs.room.runtime); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4"); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
dependencies {
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 testImplementation(project(":core:game-engine"))
 testImplementation("androidx.test.ext:junit:1.3.0")
}
android { defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" } }
dependencies {
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
}

dependencies { implementation(project(":core:content")); implementation(project(":core:network")); implementation(libs.serialization) }
