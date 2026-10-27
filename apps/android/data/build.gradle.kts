plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.serialization) }
android { namespace = "com.lughzdan.data"; compileSdk = 36
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":domain")); implementation(project(":core:database")); implementation(project(":core:network")); implementation(libs.serialization); implementation(libs.coroutines); implementation("androidx.work:work-runtime-ktx:2.10.5"); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
dependencies { implementation(libs.room.ktx) }
