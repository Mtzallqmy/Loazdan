plugins { alias(libs.plugins.kotlin.jvm); alias(libs.plugins.kotlin.serialization) }
kotlin { jvmToolchain(17) }
dependencies {
 implementation(libs.serialization)
 implementation("org.bouncycastle:bcprov-jdk15to18:1.85.2")
 implementation("io.github.erdtman:java-json-canonicalization:1.1")
 testImplementation(libs.junit)
}
