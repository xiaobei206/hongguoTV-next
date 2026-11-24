# 开发日志（持续累积）

## 2026-11-24：仓库审计、工程基础与核心迁移

### 仓库审计
- `hongguoTV-next` 初始为空仓库，默认分支为 `main`；新版所有写入仅发生在该仓库。
- `kaixinguo-android` 与旧 `hongguoTV` 均包含要求核查的主要模型、播放、CENC、缓存和诊断/接力代码。旧版主 UI 真实入口仍是大型 `MainActivity.kt` 和 RecyclerView adapter 流程，不能因 UI 目录存在就认为它运行中。
- 开心果项目 Gradle version catalog 使用 AGP 9.5.1、Kotlin 2.4.21、Compose BOM 2026.09.00、Activity 1.13.0、Core 1.19.1、lifecycle 2.11.0、Media3 1.11.1、openhongguo 0.4.0、coroutines 1.11.0、JUnit 4.13.2、org.json 20250517；Gradle Wrapper 9.8.2。新版沿用参考项目实际使用的 Compose BOM 2026.09.00、Core 1.19.1、Lifecycle 2.11.0 等版本；CI 实际验证出它们的 AAR 元数据要求 `compileSdk 37`。为避免未经验证地降级多个核心库，改为从 SDK Beta 通道安装 API37 编译平台，`targetSdk` 仍为 36，应用运行时目标行为不随 compileSdk 提高而改变。Android 17/API37 当前为 Beta，因此此构建基线的编译平台仍是预览通道，需在后续稳定版发布后再复核。
- 开心果的 `Feed` 具备 ID 去重、独立游标、加载/错误状态、退避重试及首页缓存恢复；`AppModel` 分别缓存首页、榜单、新剧和搜索 Feed。新版本沿用这些模型与状态语义，而非另造一套数据访问层。
- `CoverStore` 包含 96 MiB 内存 LRU、磁盘缩放 JPEG、同 URL 请求共享与并发 IO 限制。播放核心由 Media3 ExoPlayer、`StreamRegistry`、`CencDataSource` 和 CENC 解密组成。
- 旧 TV 服务在 NSD 上广播 `_hongguotv._tcp.`，TCP 一行一个 JSON，协议版本 1。播放请求字段：`protocol`、`type=play`、`transferId`、`targetDeviceId`、`seriesId`、`episode`、`positionMs`；回复字段：`protocol`、`transferId`、`accepted`、`message`、`deviceId`。服务在 Activity 前台期间发布可用状态，接力回复等待实际 Player 状态。此协议现在已提取纯编解码器并补充单元测试，仍需要真机互通验证。
- 两个参考公开仓库目前都有被跟踪的根目录 JKS/签名配置。新版不复制或继承其中任何密钥，使用独立签名配置；私密签名材料在新版由 `.gitignore` 排除。

### 本阶段提交内容
- 独立工程、Gradle Wrapper/版本目录、TV Manifest、根目录签名配置读取、`.gitignore`、CI 及中文文档。
- 迁移开心果的 `AppModel`、`Card`、`Catalog`、`Danmaku`、`Feed`、`PlaybackFailure`、`Player`、`Seasons`、`SeriesPage`、`Session`、`Store`、`CoverStore`、`Cenc`、`ExoVideo`、`PlaybackDiagnostics`、`PlaybackService` 和 `Streams`，包名全部改为新版命名空间。
- 迁移旧 TV 的 `TvHandoffServer` 与 `PhoneDiagnosticClient`；没有改动旧 TV 仓库。没有将开心果手机端的 `PhoneDiagnosticReceiver` 错当作 TV 端服务启动。
- 接入 Compose 浏览流程，覆盖首页、榜单、新剧、搜索、收藏、历史、影视详情、选集、播放操作、设备接力、诊断与设置。首批数据到达即可显示，Feed 采用去重/独立状态并按两屏目标补载；开始播放时取消 Feed 和在途封面加载。
- 为 Feed 分页和 TCP JSON 接力协议加入单元测试；迁移开心果已有 CENC、Catalog、弹幕、播放失败与 Session 测试。SessionTest 同时验证原有按 Tab 保存页面路径的 Router，因此继续迁移该纯状态路由模型，不搬入手机 UI；TV 主导航目前仍是独立的 TvTab/TvScreen，后续应统一 session 恢复与 TV 焦点/滚动恢复逻辑。

### 验证与未解决项
- Git 仓库审计、文件迁移及提交：已执行。
- 第一次 Actions 运行在 Android SDK setup 阶段失败，旧的 setup-android action 请求了已不再存在的 SDK 包 `tools`；随后发现 runner 的稳定 SDK channel 没有 `platforms;android-37`；将 compileSdk 降到 36 后，Gradle 依赖元数据检查又显示 14 个 AndroidX 依赖（Compose 1.12.1、Core 1.19.1、Lifecycle 2.11.0）要求 API37。为保留参考项目已使用的稳定库版本，CI 改为从 Android SDK 的 canary-inclusive channel 安装官方预览包名 `platforms;android-37.0` 与 Build Tools 37，同时 `targetSdk` 保持 36。Android 官方说明 Android 17 预览平台在 SDK Manager 中标为 Cinnamon Bun Preview，Build Tools 37 需选 37.x.x；API37 的平台包名和稳定 API 等级名不完全相同。此前失败发生在 Kotlin 编译前。
- Gradle 构建、依赖解析、Lint 与测试：最新工作流需重新运行确认，不在源码提交后预先宣称通过。
- `openhongguo` AAR 的 JNI/ABI：CI 会检查解析到的 AAR 候选和 APK 内原生库目录；仍须检查具体结果。
- x86_64 模拟器安装、ARM64 电视安装/解码、遥控器长时间焦点行为、实际播放流与接力跨设备通信：尚未通过真机/模拟器验证。
- 正式发布密钥暂未配置。Debug 测试构建与正式签名更新路径是两回事。
- 下一步重点：观察首轮 CI，修复任何构建/单测问题；验证 ABI；补全返回焦点/滚动位置恢复、后台预载调度与性能观察；真机测试接力和 CENC 播放；再创建真实的测试 Release 与 SHA-256。


## 2026-11-24：CI 故障可观测性与可选正式签名

- 第 12 次 Actions 的组合 `testDebugUnitTest lintDebug` 步骤长时间未给出可读日志/终态，因此不把该运行作为成功或失败依据；后续 workflow 将测试、Lint 拆开，避免两类任务互相遮蔽结果。
- 新 workflow 使用官方 Gradle setup action 缓存依赖；SDK/Wrapper 检查仍单独执行。
- 加入完整的 AAR JNI 条目盘点、三个目标 ABI 单独 APK 输出检查、Universal APK 验证、测试证书信息与 SHA-256 文件。
- CI 正式签名设计为可选但严格：四个 Secrets 全空时只出测试签名包；部分配置时报错；四项齐全时将 JKS 与 `signing-secrets.txt` 安全还原到仓库根目录，调用与本地一致的 Gradle release signing，验证证书并计算 SHA-256；临时文件始终在结束步骤清理，且不上传到 Artifact。
- 截止本日志条目，以上 workflow 变更已提交，必须以新一次 Actions 的最终结果验证。APK/Release/SHA 尚未宣称成功。
