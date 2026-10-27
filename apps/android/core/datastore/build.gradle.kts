plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android { namespace = "com.lughzdan.preferences"; compileSdk = 36
 defaultConfig { minSdk = 26;  }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 
 testOptions { unitTests.isIncludeAndroidResources = true }
}
kotlin { jvmToolchain(17) }
dependencies { implementation("androidx.datastore:datastore-preferences:1.1.7"); implementation(libs.coroutines); testImplementation(libs.junit); testImplementation("org.robolectric:robolectric:4.16"); testImplementation("androidx.test:core:1.7.0"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
