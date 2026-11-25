# 项目交接索引

- **仓库：** https://github.com/xiaobei206/hongguoTV-next
- **主分支：** `main`
- **版本：** `0.1.15`（恢复已确认的第二款原始图标底稿；应用图标、首页标志与 TV banner 统一使用原图素材；保留详情页浅色字和失焦后取消常驻金边修复；待 CI 与实机验收）
- **兼容策略：** 一个代码库、一个 Universal APK；当前目标 minSdk API 23，不预先拆分 Legacy / Standard。v0.1.4 是此前正式发布版本。
- **应用 ID：** `com.xiaobei206.hongguotvnext`
- **参考项目：** [开心果 Android](https://github.com/xiaobei206/kaixinguo-android)；旧 TV 只读参考：[hongguoTV](https://github.com/xiaobei206/hongguoTV)

## 重要文件
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/MainActivity.kt`：TV 主流程、搜索热搜/日期标签和设备接力回调。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/TvPlayerScreen.kt`：电视播放器遥控状态机、自动隐藏控制栏与 STATS FOR NERDS 悬浮层；视频 TextureView 缩放/平移与 UI 叠层校准分开。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/DisplayCalibration.kt`：校准参数、四边独立扩展范围以及测试框几何计算。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/DisplayCalibrationScreen.kt`：界面/视频的全屏高对比测试图和遥控器校准按钮。
- `.github/workflows/publish-v0.1.12.yml`：v0.1.12 历史正式发布流程。
- `.github/workflows/publish-v0.1.13.yml`：v0.1.13 历史正式发布流程。
- `.github/workflows/publish-v0.1.14.yml`：v0.1.14 历史正式发布流程。
- `.github/workflows/publish-v0.1.15.yml`：仅在 v0.1.15 最终标记提交对应的 Android CI 成功后，校验正式签名 APK、证书与 SHA-256 并创建正式 Release。
- `app/src/main/kotlin/com/xiaobei206/hongguotvnext/TvApplication.kt`：SDK、AppModel、CoverStore、Media3 和接力服务生命周期。
- `model/`：开心果的模型、Feed、分页、收藏/历史、搜索和播放状态机。
- `covers/CoverStore.kt`：封面内存/磁盘缓存与 URL 请求去重。
- `cenc/Cenc.kt` 与 `playback/Streams.kt`：CENC 读取与媒体字节流。
- `playback/ExoVideo.kt`、`PlaybackDiagnostics.kt`、`PlaybackService.kt`：媒体播放、诊断与 MediaSessionService。
- `TvHandoffServer.kt`：旧 TV 的 TCP/NSD 接力兼容实现；`TvHandoffProtocolTest.kt` 测试协议编解码。
- `PhoneDiagnosticClient.kt`：向发现到的开心果手机发送电视诊断报告。
- `app/build.gradle.kts`：版本、依赖、ABI split 和根目录签名读取。
- `gradle/libs.versions.toml`：版本目录；Maven 0.4.1 只作非打包配置回退，TV APK 必须使用固定自维护 SDK `1e9334185a46033bcc4e3e30fc41f649958b1389`.
- `scripts/patch-openhongguo.py`：对固定 SDK 应用结构化诊断扩展。
- `.github/workflows/android.yml`：固定检出 SDK 源码、运行 Go 回归测试、编译 AAR，并将同一 AAR 注入 NEXT 所有 APK 构建。
- `.github/workflows/android.yml`：单测、Lint、Debug 构建、ABI 观察及 APK/SHA Artifact。
- `.github/workflows/publish-v0.1.11.yml`：仅当 v0.1.11 最终标记提交的 Android CI 全部成功后，下载同一次 CI 的正式签名 APK，校验证书和 SHA-256，再创建 `v0.1.11` GitHub Release。
- `DEVELOPMENT-LOG-CN.md`、`CHANGELOG.md`、`SIGNING-CN.md`：日志、版本记录和签名说明。

## 播放 SDK 固定版本

- `xiaobei206/openhongguo@1e9334185a46033bcc4e3e30fc41f649958b1389` 是当前 TV APK 必须使用的 SDK 源码。
- 官方 Maven `0.4.1` 只保留给 IDE 同步/非打包配置；APK 打包没有 `-PpatchedOpenhongguoAar=<path>` 时会被 Gradle 显式拒绝。
- 构建顺序：SDK Go tests → diagnostics patch → mobile core Go tests → gomobile AAR → NEXT unit tests/Lint/ABI/Debug/Release。
- 核心回退为 V5 空响应 → 同一身份 signed bodyless `video_tab` GET → 旧接口兼容回退，并严格匹配当前分集。

## 构建与测试现状

- Android 6.0 兼容改动：minSdk=23，启用 Core Library Desugaring 2.1.5；API 23-27 封面解码、JSON 原子替换、NSD resolving 集合均有旧系统实现；CI 检查最终 APK 的 sdkVersion 必须为 23。CI 成功仍不等于 E55A / 75U6900C 真机播放已通过。
- 当前工程版本为 `0.1.8`，使用独立 applicationId，可与旧版并行安装；minSdk 目标为 API 23。
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


## v0.1.11 影院石墨主题与播放器焦点改进
- 全局配色统一为深石墨、墨蓝灰、古铜金、琥珀金、暖白和雾灰；导航、筛选、季数、选集、设置、设备接力和校准控件使用一致的琥珀焦点样式。
- 播放菜单出现时直接聚焦“播放/暂停”；左右键直接浏览按钮，上下键在菜单聚焦期间不会把焦点带离控制区；OK 打开菜单时不再顺带暂停视频。
- 播放器操作按钮统一为大尺寸高对比控件，当前焦点使用琥珀底、暖白粗描边、深色文字和轻微放大；菜单无操作 12 秒后收起，焦点移动会重新计时。
- 版本为 versionCode 12 / versionName 0.1.11；CI 自动构建与电视实机遥控验收完成前，不宣称已通过最终验收。

## v0.1.10 四边独立画面校准
- 先按 5 个百分点等比例缩放（70%–100%），再每次 0.5% 只向外扩展上/下/左/右指定边缘；没有整体平移动作。
- 界面和视频分两张全屏高对比测试图校准，独立自动保存，支持本项恢复和一键全部恢复。焦点以亮红色、白色粗边框与放大显示。
- 视频通过全屏 PlayerView 视口的 graphicsLayer 与测试框使用相同的边界公式；默认 100% 保留 PlayerView 的 fit 比例，独立扩边后允许非等比例拉伸。旧电视实际边缘、字幕/缓冲层和遥控器焦点仍需实机验收。

## v0.1.9 老电视画面校准（初版，后由 v0.1.10 重做）
- 初版提供等比例缩放和位置调整；实际电视操作反馈显示控件密集、焦点对比不足、方向逻辑错误，v0.1.10 改为四边独立扩展并使用高对比测试画面。

## v0.1.8 开心果三图诊断面板
- 诊断面板按开心果原实现显示 Connection Speed、Network Activity、Buffer Health 三条图，统一字号、颜色、底板和扫描游标。
- 连接速度为蓝色平滑曲线，网络活动为分档柱形，缓冲健康度为填充曲线；采样窗口固定为 20 秒 / 40 个 500ms 槽位。

## v0.1.6 同系列多季选择
- 在详情页选集上方增加同系列季数栏；任意一季的详情都共享排序后的季列表。
- 当前季高亮，其他已上线季可用遥控器直接切换；无分集季保留显示但不可点击。
- 季列表加载状态改为可观察状态，等待同系列信息时可显示加载提示。

## v0.1.5 封面修复
- 对照旧 TV 的 `TvArtworkLoader.candidateUrls`，发现 NEXT 迁移时漏掉了 ByteDance CDN 的 JPEG 回退；本版恢复该逻辑，并增加原始 signed URL 后备。
- 诊断页现在会显示封面成功/失败数量及最近失败原因。
- 需在小米 E55A、E55S 和东芝 75U6900C 实机检查首页、排行榜、新剧、热搜和详情页封面。
