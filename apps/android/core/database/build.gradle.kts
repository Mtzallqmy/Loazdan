plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.ksp) }
android { namespace = "com.lughzdan.db"; compileSdk = 36
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation(libs.room.runtime); implementation(libs.room.ktx); ksp(libs.room.compiler); implementation(libs.coroutines); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
