import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val patchedOpenhongguoAar = providers.gradleProperty("patchedOpenhongguoAar").orNull
val patchedOpenhongguoAarFile = patchedOpenhongguoAar?.let { rootProject.file(it) }
val appPackagingRequested = gradle.startParameter.taskNames.any { taskName ->
    val task = taskName.substringAfterLast(':')
    task.equals("assemble", ignoreCase = true) ||
        task.startsWith("assembleDebug", ignoreCase = true) ||
        task.startsWith("assembleRelease", ignoreCase = true) ||
        task.startsWith("packageDebug", ignoreCase = true) ||
        task.startsWith("packageRelease", ignoreCase = true) ||
        task.startsWith("bundle", ignoreCase = true)
}
if (appPackagingRequested && patchedOpenhongguoAarFile == null) {
    throw GradleException(
        "红果 TV Next 的安装包必须使用自维护 openhongguo AAR。请先按 README.md 构建 SDK，" +
            "再添加 -PpatchedOpenhongguoAar=<openhongguo-release.aar>；不能回退到官方 Maven SDK。"
    )
}
if (appPackagingRequested && patchedOpenhongguoAarFile?.isFile != true) {
    throw GradleException("找不到自维护 openhongguo AAR：${patchedOpenhongguoAarFile?.absolutePath}")
}
val releaseKeyFile = rootProject.file("hongguoTV-next-release.jks")
val signingSecretsFile = rootProject.file("signing-secrets.txt")
val signingSecrets = Properties().apply {
    if (signingSecretsFile.isFile) {
        try {
            signingSecretsFile.inputStream().use(::load)
        } catch (e: Exception) {
            throw GradleException("无法读取项目根目录 signing-secrets.txt；请检查文件编码和 properties 格式。", e)
        }
    }
}
val signingKeys = listOf(
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)
val hasAnySigningInput = releaseKeyFile.exists() || signingSecretsFile.exists()
val missingSigningParameters = signingKeys.filter { signingSecrets.getProperty(it).isNullOrBlank() }
val releaseSigningConfigured = releaseKeyFile.isFile &&
    signingSecretsFile.isFile &&
    missingSigningParameters.isEmpty()
val releasePackageRequested = gradle.startParameter.taskNames.any { taskName ->
    val task = taskName.substringAfterLast(':')
    task.startsWith("assembleRelease", ignoreCase = true) ||
        task.startsWith("bundleRelease", ignoreCase = true) ||
        task.startsWith("packageRelease", ignoreCase = true)
}

if (!releaseSigningConfigured && releasePackageRequested) {
    val problems = buildList {
        if (!releaseKeyFile.isFile) add("缺少项目根目录 hongguoTV-next-release.jks")
        if (!signingSecretsFile.isFile) add("缺少项目根目录 signing-secrets.txt")
        if (missingSigningParameters.isNotEmpty()) {
            add("signing-secrets.txt 缺少参数：${missingSigningParameters.joinToString()}")
        }
    }
    throw GradleException(
        "不能生成正式签名的 Release APK：${problems.joinToString("；")}。请先配置正式密钥；" +
            "当前仍可运行 assembleDebug 生成测试签名 APK。"
    )
}

if (hasAnySigningInput && !releaseSigningConfigured) {
    logger.warn(
        "检测到不完整的发布签名输入；Debug 构建/测试继续使用 Android Debug 测试证书。" +
            "请补齐项目根目录 JKS 与 signing-secrets.txt；Debug 证书不会被当作正式签名。"
    )
} else if (!releaseSigningConfigured) {
    logger.lifecycle("正式发布签名尚未配置；Debug APK 使用 Android Debug 测试证书。")
}

android {
    namespace = "com.xiaobei206.hongguotvnext"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xiaobei206.hongguotvnext"
        // API 23 keeps one installable build for Android 6.0-era TVs.
        minSdk = 23
        targetSdk = 36
        versionCode = 34
        versionName = "0.1.33"
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
                // Explicit release packaging tasks are rejected above without formal credentials.
                logger.lifecycle("Release signing is not configured; only a debug test-signed APK can be produced.")
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
        // java.time is used by the sleep timer and diagnostic timestamps.
        // Desugar it so those code paths also work on Android 6.0 (API 23).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Media3/Compose opt-in markers are compiler-checked; Android Lint currently misreports
        // valid Kotlin @file:OptIn usage in the migrated playback sources, matching the reference app.
        disable += "UnsafeOptInUsageError"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}


/*
 * Generate a transparent splash-screen variant from the existing full-size launcher artwork.
 * The script removes only border-connected near-black background pixels and does not resize/crop.
 */
val splashIconSource = layout.projectDirectory.file("src/main/res/drawable-nodpi/tv_icon_launcher.webp")
val splashIconGenerator = rootProject.file("scripts/create-transparent-splash-icon.py")
val generatedSplashResDir = layout.buildDirectory.dir("generated/splash-icon/res")
val generatedSplashIcon = generatedSplashResDir.map { it.file("drawable-nodpi/tv_splash_icon.png") }
val generateTransparentSplashIcon by tasks.registering(org.gradle.api.tasks.Exec::class) {
    inputs.file(splashIconSource)
    inputs.file(splashIconGenerator)
    outputs.file(generatedSplashIcon)
    doFirst {
        generatedSplashIcon.get().asFile.parentFile.mkdirs()
    }
    commandLine(
        providers.gradleProperty("pythonExecutable").orElse("python3").get(),
        splashIconGenerator.absolutePath,
        splashIconSource.asFile.absolutePath,
        generatedSplashIcon.get().asFile.absolutePath
    )
}
android.sourceSets.getByName("main").res.srcDir(generatedSplashResDir)
tasks.named("preBuild").configure {
    dependsOn(generateTransparentSplashIcon)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    if (patchedOpenhongguoAarFile != null) {
        implementation(files(patchedOpenhongguoAarFile))
    } else {
        // Only IDE sync/non-packaging tasks can use the Maven fallback.
        implementation(libs.openhongguo) { artifact { type = "aar" } }
    }
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
