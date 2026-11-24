import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseKeyFile = rootProject.file("hongguoTV-next-release.jks")
val signingSecretsFile = rootProject.file("signing-secrets.txt")
val signingSecrets = Properties().apply {
    if (signingSecretsFile.isFile) {
        signingSecretsFile.inputStream().use(::load)
    }
}
val signingKeys = listOf(
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)
val hasAnySigningInput = releaseKeyFile.exists() || signingSecretsFile.exists()
val releaseSigningConfigured = releaseKeyFile.isFile &&
    signingSecretsFile.isFile &&
    signingKeys.all { !signingSecrets.getProperty(it).isNullOrBlank() }

if (hasAnySigningInput && !releaseSigningConfigured) {
    throw GradleException(
        "发布签名配置不完整。必须同时在项目根目录提供 hongguoTV-next-release.jks 与 signing-secrets.txt，" +
            "且后者必须包含 ANDROID_KEYSTORE_PASSWORD、ANDROID_KEY_ALIAS、ANDROID_KEY_PASSWORD。"
    )
}

android {
    namespace = "com.xiaobei206.hongguotvnext"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xiaobei206.hongguotvnext"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = releaseKeyFile
                storePassword = signingSecrets.getProperty("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingSecrets.getProperty("ANDROID_KEY_ALIAS")
                keyPassword = signingSecrets.getProperty("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Debug builds are explicitly test-signed by Android's debug key.
        }
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Do not silently substitute the debug certificate for release signing.
                logger.lifecycle(
                    "Release signing is NOT configured. assembleRelease will be unsigned; " +
                        "use assembleDebug for an installable test APK."
                )
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.openhongguo) { artifact { type = "aar" } }
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.coroutines.test)
}
