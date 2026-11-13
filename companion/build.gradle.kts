plugins {
    id("com.android.library")
}

android {
    namespace = "com.xucong.wordmemory.companion"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")

    // 桥接契约是纯 Kotlin，单测不用 Robolectric/模拟器
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
