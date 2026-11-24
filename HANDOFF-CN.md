# 项目交接索引

- **仓库：** https://github.com/xiaobei206/hongguoTV-next
- **主分支：** `main`
- **当前版本：** `0.1.0`（初始化阶段，尚未发布）
- **应用 ID：** `com.xiaobei206.hongguotvnext`
- **参考项目：** [开心果 Android](https://github.com/xiaobei206/kaixinguo-android)；旧 TV 只读参考：[hongguoTV](https://github.com/xiaobei206/hongguoTV)

## 文件索引
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/MainActivity.kt`：电视主界面入口。
- `app/src/main/AndroidManifest.xml`：TV Launcher、网络及播放权限。
- `app/build.gradle.kts`：依赖、版本、ABI split 与根目录签名读取。
- `gradle/libs.versions.toml`：统一依赖版本。
- `.github/workflows/android.yml`：CI、测试、Debug APK 与 SHA-256 Artifact。
- `DEVELOPMENT-LOG-CN.md`：累计开发日志。
- `CHANGELOG.md`：按版本记录已完成内容。
- `SIGNING-CN.md`：本地/CI 签名说明。

## 当前状态
- 工程骨架：已提交，待 CI 确认能编译。
- 开心果数据层、Feed/缓存/Media3/CENC：在迁移过程中，尚未验收。
- 旧 TV 接力服务及手机兼容：协议已审计，尚未在新版移植或测试。
- ABI：Gradle 配置包含 x86_64、arm64-v8a、armeabi-v7a 并产生 universal 输出的意图；尚未通过 AAR 检查、APK 内容检查或模拟器安装验证。
- 签名：尚无新版独立正式密钥；本地正式密钥不配置也可以进行 Debug 构建。不能保证测试 APK 可覆盖安装正式版本。
- GitHub Actions Secrets：未确认存在，必需名称为 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。
- APK / SHA-256 / Release：待成功构建后更新本索引。
- 未解决问题与下一阶段：迁移模型及播放器；实现 TV 专用完整导航、列表分页、详情/搜索/收藏/历史/选集；迁移接力；补充单测与 ABI 审计。
