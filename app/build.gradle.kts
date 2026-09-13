plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 固定签名：本地与 CI 共用同一 keystore，保证 APK 可互相覆盖安装。
// CI 通过环境变量注入（release.yml 从 Secrets 解码），本地默认使用 keystore/ 目录下的文件。
val sharedStoreFile = System.getenv("SIGNING_STORE_FILE")?.let { rootProject.file(it) }
    ?: rootProject.file("keystore/radiofm-release.keystore")
val sharedStorePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: "RadioFm2026"
val sharedKeyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "radiofm"
val sharedKeyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: sharedStorePassword

android {
    namespace = "com.huanglongmao.onlinefmradio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.huanglongmao.onlinefmradio"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
    }

    signingConfigs {
        if (sharedStoreFile.exists()) {
            create("shared") {
                storeFile = sharedStoreFile
                storePassword = sharedStorePassword
                keyAlias = sharedKeyAlias
                keyPassword = sharedKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 优先固定签名；keystore 缺失时退回 debug 签名保证能构建
            signingConfig = signingConfigs.findByName("shared")
                ?: signingConfigs.getByName("debug")
        }
        debug {
            signingConfigs.findByName("shared")?.let { signingConfig = it }
        }
    }

    // 传 -PabiSplits 时按架构分包并附 universal 包（CI 发布用）
    if (project.hasProperty("abiSplits")) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "armeabi-v7a", "x86_64")
                isUniversalApk = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // 生成 BuildConfig，APP_VERSION 直接读取 versionName，避免硬编码漂移
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
