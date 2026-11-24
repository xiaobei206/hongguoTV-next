# 项目交接索引

- **仓库：** https://github.com/xiaobei206/hongguoTV-next
- **主分支：** `main`
- **版本：** `0.1.2`（搜索热榜、日期标签、电视播放遥控与播放诊断）
- **发布候选标记：** `release: prepare v0.1.2 final`；该提交的 Android CI、正式签名与 SHA-256 校验均已成功，Release 已发布。
- **应用 ID：** `com.xiaobei206.hongguotvnext`
- **参考项目：** [开心果 Android](https://github.com/xiaobei206/kaixinguo-android)；旧 TV 只读参考：[hongguoTV](https://github.com/xiaobei206/hongguoTV)

## 重要文件
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/MainActivity.kt`：TV 主流程、搜索热搜/日期标签和设备接力回调。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/TvPlayerScreen.kt`：电视播放器遥控状态机、自动隐藏控制栏与 STATS FOR NERDS 悬浮层。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/TvApplication.kt`：SDK、AppModel、CoverStore、Media3 和接力服务生命周期。
- `model/`：开心果的模型、Feed、分页、收藏/历史、搜索和播放状态机。
- `covers/CoverStore.kt`：封面内存/磁盘缓存与 URL 请求去重。
- `cenc/Cenc.kt` 与 `playback/Streams.kt`：CENC 读取与媒体字节流。
- `playback/ExoVideo.kt`、`PlaybackDiagnostics.kt`、`PlaybackService.kt`：媒体播放、诊断与 MediaSessionService。
- `TvHandoffServer.kt`：旧 TV 的 TCP/NSD 接力兼容实现；`TvHandoffProtocolTest.kt` 测试协议编解码。
- `PhoneDiagnosticClient.kt`：向发现到的开心果手机发送电视诊断报告。
- `app/build.gradle.kts`：版本、依赖、ABI split 和根目录签名读取。
- `gradle/libs.versions.toml`：统一依赖版本；openhongguo 已升级到 0.4.1。
- `.github/workflows/android.yml`：单测、Lint、Debug 构建、ABI 观察及 APK/SHA Artifact。
- `DEVELOPMENT-LOG-CN.md`、`CHANGELOG.md`、`SIGNING-CN.md`：日志、版本记录和签名说明。

## 构建与测试现状
- 当前工程版本为 `0.1.2`，使用独立 applicationId，可与旧版并行安装。
- [v0.1.2 正式构建](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45389071879) 全部成功：单测、Lint、Debug APK、ABI 检查、正式 Release APK、签名证书验证及 SHA-256。早期基线 [Actions #20](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45346930533) 作为 v0.1.0 历史记录保留。
- Actions 已将测试与 Lint 分成独立步骤并加入 Gradle 缓存；完整配置可选的四个签名 Secrets 后可走正式签名 Artifact 路径。
- 开心果的数据模型、Feed、存储、播放/CENC、封面缓存及核心测试已迁移；实际构建结果以 [Actions](https://github.com/xiaobei206/hongguoTV-next/actions) 为准。
- v0.1.2 的遥控器映射已实现但尚未真机验收：菜单隐藏时左右快退/快进、上下切集；OK 首次唤出菜单，下一次暂停并聚焦播放按钮；控制栏可导航；菜单超时后不再组合任何控制按钮。
- 接力协议已经保留关键字段并增加纯协议测试，尚不能据此宣称与实际手机通信通过。
- 当前 Gradle 实际产出 Universal APK。CI 会检查该 APK 内 x86_64、arm64-v8a、armeabi-v7a 的 native libraries 以及每种 ABI 的 openhongguo libgojni.so；模拟器/真机安装仍是后续验收项。
- v0.1.0 正式签名 APK：`hongguoTV-next-v0.1.0-universal-release-signed.apk`；SHA-256：`11145b0b612765dfaef40b57e191d54a63423071058c8a393c6b43e22b3c9dbb`。
- v0.1.2 已正式发布：[下载正式签名 APK](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.2)。CI 已通过单测、Lint、ABI、正式签名与 SHA-256；新播放器尚未在目标电视上实测。
- 正式证书 SHA-256：`ebf27c606826ad1ae5ac72a2ae31751b2769c2c0029b3ac4d1e748d3daa57a34`。
- Actions Run #20 已完成：[查看构建记录与 Artifact](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45346930533)。v0.1.0 正式 Release：[打开发布页](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.0)。

## 签名状态
- 新版独立签名密钥已实际创建并提交于仓库根目录：`hongguoTV-next-release.jks`；对应配置在根目录 `signing-secrets.txt`。CI 已改为优先读取这两份文件，且已通过正式签名构建。
- 本项目按自用约定将 `hongguoTV-next-release.jks`、`signing-secrets.txt` 实际保留在仓库根目录并提交；CI 使用它们签名，Artifact 不包含密钥文件。
- 正式签名当前直接使用仓库根目录文件，不需配置 GitHub Actions Secrets；备用 Secrets 名称仍保留在 Workflow 中。
- CI 输出 Debug 测试签名 APK 与正式签名 Release APK。两者证书不同；测试版不能直接覆盖安装正式签名版。

## 下一阶段
1. 确认 openhongguo 0.4.1 后的真实播放结果，并在设备上回归测试。
2. 在 x86_64 模拟器安装正式签名 APK，验证首次启动、Feed、返回、焦点及页面状态恢复。
3. 在 ARM 电视安装测试，实际验证 CENC 视频播放、清晰度切换、解码器与播放期间后台任务影响。
4. 用实际手机测试 `_hongguotv._tcp.` 服务发现、接力请求与成功/失败确认。
5. 处理 `libgojni.so` 的 16 KB 页面对齐警告；当前仅记录，尚未解决。
6. 在目标电视上验收 v0.1.2 播放/遥控器，并用真实手机回归设备接力。
