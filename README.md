# 红果 TV Next

**独立的 Android TV 重开发项目。** 代码只写入此仓库；旧版 [hongguoTV](https://github.com/xiaobei206/hongguoTV) 保持只读，手机/平板项目 [kaixinguo-android](https://github.com/xiaobei206/kaixinguo-android) 是核心架构参考。

## 目标

- Kotlin + Jetpack Compose 的 TV 专用界面，优先适配遥控器与大屏观看距离。
- 迁移并适配开心果已经使用的 Feed、AppModel、缓存、Media3 / ExoPlayer、流处理和 CENC 组件。
- 保留旧 TV 的投屏接力协议，并通过协议测试与实际设备验证分别报告状态。
- 独立 applicationId：`com.xiaobei206.hongguotvnext`，用于与旧版本并行安装。
- Universal APK 尽可能包含 `x86_64`、`arm64-v8a`、`armeabi-v7a`，最终支持情况以实际 AAR/ APK ABI 检查和模拟器安装结果为准。

## 构建

需要 JDK 17 或兼容的较新 JDK、Android SDK Platform 37，以及网络访问 Google Maven、Maven Central 和 Gradle Plugin Portal。

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell 使用 `.gradlew.bat testDebugUnitTest lintDebug assembleDebug`。

测试 APK 默认使用 Android Debug 签名，不属于正式发布签名。构建产物与校验值由 GitHub Actions 上传到对应 workflow run 的 Artifact。

## 根目录签名布局

如需本地发布签名，在**仓库根目录**放置 `hongguoTV-next-release.jks` 和 `signing-secrets.txt`。签名配置格式只包含以下键名（值由维护者本地填写）：

```properties
ANDROID_KEYSTORE_PASSWORD=
ANDROID_KEY_ALIAS=
ANDROID_KEY_PASSWORD=
```

两个文件都被 `.gitignore` 排除。不能提交到公开仓库、构建日志、文档或普通 Artifact。缺少签名时，Debug 测试构建仍可使用；Gradle 不会静默用 Debug 证书签署 Release APK。

CI 正式签名预留 Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。目前未声明这些 Secrets 已配置。

## 状态声明

当前处于工程启动与迁移阶段。Compose shell 能构建不代表内容接口、播放器、CENC、投屏接力或性能已验证；每一项会在开发日志与交接索引中区分自动化测试、构建验证和真机测试。
