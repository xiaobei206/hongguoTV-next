# 项目交接索引

- **仓库：** https://github.com/xiaobei206/hongguoTV-next
- **主分支：** `main`
- **版本：** `0.1.0`（开发中，尚未 Release）
- **应用 ID：** `com.xiaobei206.hongguotvnext`
- **参考项目：** [开心果 Android](https://github.com/xiaobei206/kaixinguo-android)；旧 TV 只读参考：[hongguoTV](https://github.com/xiaobei206/hongguoTV)

## 重要文件
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/MainActivity.kt`：TV 主流程和设备接力回调。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/TvApplication.kt`：SDK、AppModel、CoverStore、Media3 和接力服务生命周期。
- `model/`：开心果的模型、Feed、分页、收藏/历史、搜索和播放状态机。
- `covers/CoverStore.kt`：封面内存/磁盘缓存与 URL 请求去重。
- `cenc/Cenc.kt` 与 `playback/Streams.kt`：CENC 读取与媒体字节流。
- `playback/ExoVideo.kt`、`PlaybackDiagnostics.kt`、`PlaybackService.kt`：媒体播放、诊断与 MediaSessionService。
- `TvHandoffServer.kt`：旧 TV 的 TCP/NSD 接力兼容实现；`TvHandoffProtocolTest.kt` 测试协议编解码。
- `PhoneDiagnosticClient.kt`：向发现到的开心果手机发送电视诊断报告。
- `app/build.gradle.kts`：版本、依赖、ABI split 和根目录签名读取。
- `gradle/libs.versions.toml`：统一依赖版本。
- `.github/workflows/android.yml`：单测、Lint、Debug 构建、ABI 观察及 APK/SHA Artifact。
- `DEVELOPMENT-LOG-CN.md`、`CHANGELOG.md`、`SIGNING-CN.md`：日志、版本记录和签名说明。

## 构建与测试现状
- 工程版本设置为 `0.1.0`，使用独立 applicationId，可与旧版并行安装。
- 开心果的数据模型、Feed、存储、播放/CENC、封面缓存及核心测试已迁移；实际构建结果以 [Actions](https://github.com/xiaobei206/hongguoTV-next/actions) 为准。
- UI 接通当前核心流程；真实焦点循环、返回后精确恢复焦点/滚动位置、遥控器体验需模拟器/电视测试。
- 接力协议已经保留关键字段并增加纯协议测试，尚不能据此宣称与实际手机通信通过。
- Universal/ABI 产物已在 Gradle 配置，真实兼容性以 CI APK 检查和安装测试为准。
- APK、SHA-256 和 Release 链接：首轮 CI 通过后补入此处。不能用源码 commit 替代安装包或测试证据。

## 签名状态
- 新版独立签名密钥尚未确认配置。
- 本地密钥与配置文件应放项目根目录：`hongguoTV-next-release.jks`、`signing-secrets.txt`；两个文件由 `.gitignore` 排除。
- CI 正式签名所需 Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。尚未确认已配置，不记载任何秘密值。
- 当前 CI 预期输出为 Android Debug 测试签名 APK，不能保证覆盖安装未来的正式签名 APK。

## 下一阶段
1. 处理首轮 Actions 的实际构建、Lint 或测试失败。
2. 检查 openhongguo AAR/所有 native libraries 的 x86_64、arm64-v8a、armeabi-v7a 支持。
3. 模拟器安装测试，修正焦点、列表复用、返回和页面状态恢复。
4. 用实际手机测试 `_hongguotv._tcp.` 发现、请求、成功/失败确认；用真实视频验证 CENC、清晰度、解码器与后台任务对播放的影响。
5. 通过后创建明确标记测试签名的 Release/Artifact，记录 SHA-256；正式签名就绪后再单独走正式发布路径。
