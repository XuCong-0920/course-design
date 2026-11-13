plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.xucong.wordmemory"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xucong.wordmemory"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        // 仪器化测试：把「解析 → 事务写入 Room」这条链路在真机上跑一遍
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        // 录屏水印用 BuildConfig.DEBUG 判断，release 包里不会画水印
        buildConfig = true
    }
}

dependencies {
    implementation(project(":companion"))

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.10.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Room：运行时 + 协程/Flow 支持 + KSP 注解处理器
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // WorkManager：每日复习提醒（周期任务 + 一次性延迟重发）
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // 联网：OkHttp 负责请求，Gson 负责解析词典 JSON（都用成熟库，不手搓）
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("com.google.code.gson:gson:2.15.0")

    // 纯 JVM 单元测试（SRS 调度算法等，不需要设备/模拟器）
    testImplementation("junit:junit:4.13.2")
    // 单测里用 runBlocking 测挂起函数（lookup 是 suspend）
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // 仪器化测试（真机/模拟器上跑 Room 真实写入）
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
}
