# 红果 TV Next

独立的新一代 Android TV 客户端。所有新版开发仅写入本仓库；旧版 [hongguoTV](https://github.com/xiaobei206/hongguoTV) 保持只读，手机/平板项目 [kaixinguo-android](https://github.com/xiaobei206/kaixinguo-android) 是核心架构参考。

## 当前实现

- Kotlin + Jetpack Compose 的电视端主流程：推荐、排行榜、新剧上线、搜索、收藏、历史、详情、选集、播放器、设备接力、诊断和设置入口。
- 迁移开心果的 AppModel、Catalog、Card、Feed、收藏/历史持久化、会话状态、播放状态机、CoverStore、Media3 ExoPlayer、流注册与 CENC 解密、播放诊断。
- 迁移旧 TV 的 TCP/NSD 投屏接力服务及发往开心果手机端的诊断客户端；接力协议保持版本 1，独立协议编解码器有单元测试。
- 页面首批数据先呈现，Feed 独立分页；接近末尾续载，补载目标约两屏；播放开始时取消 Feed 请求与待处理封面下载。
- 独立 applicationId：`com.xiaobei206.hongguotvnext`，用于与旧版本并行安装。

## 当前限制

核心源码已迁移到新包名，但迁移还不能等同于测试通过。GitHub Actions 构建、openhongguo AAR 原生依赖检查、x86_64 模拟器安装、ARM 电视安装、实际遥控器焦点、真实设备间接力和实际视频播放都以本仓库 CI/设备验证记录为准。还没有声明以上硬件项目全部通过。

## 构建

需要 JDK 17、Android SDK Platform 37（Beta 通道，仅供编译；targetSdk 仍为稳定 API 36），以及可以访问 Google Maven、Maven Central 和 Gradle Plugin Portal 的网络。

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell 使用 `.gradlew.bat testDebugUnitTest lintDebug assembleDebug`。

CI 将单元测试、Android Lint、Debug 构建分成独立步骤，并使用 Gradle 缓存；Debug APK 使用 Android Debug 测试证书，通过 Workflow Artifact 提供下载。测试签名 APK 不等于正式发布签名，也不保证能够覆盖安装未来正式签名版本。

若在 GitHub Actions Secrets 配齐四项正式签名 Secret，CI 还会从仓库根目录安全还原独立发布 JKS，构建并验证正式签名 Release APK，输出证书指纹与 SHA-256，并在结束后删除临时密钥文件。四项全部未配置时只生成测试签名版本；只配置其中一部分会明确失败。

## ABI

Gradle 已配置生成包含 `x86_64`、`arm64-v8a`、`armeabi-v7a` 的 ABI APK 与 Universal APK。CI 会列出打包进 APK 的原生库目录；若 `openhongguo` AAR 引入只支持部分 ABI 的 `.so`，应以实际构建、APK 内容和安装结果为准。

## 根目录签名

正式发布时把独立密钥放在**仓库根目录**：

- `hongguoTV-next-release.jks`
- `signing-secrets.txt`

属性文件内容格式：

```properties
ANDROID_KEYSTORE_PASSWORD=
ANDROID_KEY_ALIAS=
ANDROID_KEY_PASSWORD=
```

这两个文件及其他 JKS/keystore/P12/PFX 已列入 `.gitignore`，绝不能提交到公开仓库或上传为构建 Artifact。密钥缺失或只配置了部分签名文件时，Debug 测试构建、测试和 Lint 仍可执行；显式请求 Release 包时，如果正式签名不完整会报出具体缺项，不会静默用 Debug 签名冒充正式 Release 签名。

正式签名 CI 预留 Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。当前是否已配置以仓库 Actions 设置为准。

## 官方参考

- [Compose for TV](https://developer.android.com/training/tv/playback/compose)
- [TV 滚动列表与焦点行为](https://developer.android.com/training/tv/playback/compose/lists)
- [Media3 播放器与 MediaSessionService](https://developer.android.com/media/implement/playback-app)

## 项目文档

- [累计开发日志](DEVELOPMENT-LOG-CN.md)
- [版本变更](CHANGELOG.md)
- [交接索引](HANDOFF-CN.md)
- [签名说明](SIGNING-CN.md)
