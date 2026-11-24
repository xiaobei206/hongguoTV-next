# 开发日志（持续累积）

## 2026-11-24：v0.1.3 修复播放空响应与统一 SDK 构建

- 对照 NEXT 初版和开心果历史提交后确认：两边 `Catalog.resolvePlayback` 入口基本相同，差异是 NEXT 只依赖官方 Maven SDK；开心果早在 2026-11-21 就加入 V5 空响应后的签名 `video_tab` GET 恢复，并通过工作流注入本地 AAR。
- Android CI 固定检出 `xiaobei206/openhongguo@1e9334185a46033bcc4e3e30fc41f649958b1389`，执行诊断补丁，运行 `go test ./...` 和 mobile core 测试，再使用 `bindings/mobile/build.sh android` 构建 AAR。
- NEXT 的测试、Lint、Debug APK 和正式签名 APK都显式使用这份 AAR；缺少 AAR 参数时 APK 打包会失败，避免静默退回官方 Maven SDK。
- 该 SDK 版本包含 V5 空响应后的签名、无请求体 `video_tab` 回退、同一身份和当前分集校验，以及设备激活、EGDI 与最新签名实现。
- 诊断补丁仅输出 HTTP 状态、去 query 接口路径、响应长度、Content-Type/Encoding 与脱敏 profile fingerprint；不会记录原始标识、Cookie、Token、签名或完整响应体。
- 版本提高到 v0.1.3 / versionCode 4；同步 CI Artifact 与发布工作流文件名。
- 源码提交不等于验证通过；需核对本次 Actions 及在目标电视上重试此前返回空 200 的同一分集。

## 2026-11-24：v0.1.2 电视遥控器播放器、热搜和日期标签

- 新增 `TvPlayerScreen.kt`，以电视遥控器为中心管理 HUD 可见状态与焦点状态；菜单隐藏时左右快退/快进、上下切集；OK 第一次显示菜单，下一次暂停并把焦点交给播放按钮；控制栏获得焦点后允许方向键导航。
- 播放初始和切集时菜单短暂显示约 0.5 秒；手动 OK 唤出菜单 3 秒隐藏，控制按钮交互后 5 秒隐藏。菜单隐藏时控制按钮直接不进入 Compose 树，避免残留按钮。菜单显示时返回先隐藏；隐藏时返回提示二次确认退出。
- 诊断入口中文命名为“播放诊断”，悬浮框保留英文 `STATS FOR NERDS`，读取已有 `PlaybackDiagnostics` 的真实吞吐、编码、解码器、分辨率、帧率、码率和丢帧信息。
- 搜索空查询时复用 `AppModel.hot` 和开心果 SDK 热搜结构；展示排名、榜单名、热度/搜索分数，并在热搜加载失败时提供重试。
- 新剧日期按钮调用迁移自开心果的 `dayLabel(date, todayKey())`，日期筛选逻辑保持原样。
- 版本提升到 0.1.2 / versionCode 3；CI Artifact 与正式 Release 工作流更新为 v0.1.2。Actions Run [#45389071879](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45389071879) 已通过单测、Lint、Debug 构建、ABI 检查、正式签名与 SHA-256 校验；[v0.1.2 正式 Release](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.2) 已发布。播放和遥控器仍需目标电视真机验收。

## 2026-11-24：v0.1.1 播放空响应修复与接力开关

- 根据电视发回的诊断，列表/详情请求正常，但播放地址接口 `/novel/player/video_model/v1/` 返回 HTTP 200、空响应体，导致 `ANONYMOUS_LIMITED`。上游 openhongguo 在 2026-11-24 发布 0.4.1，说明 0.4.0 在服务端于 11 月 23 日起拒绝带模拟器状态值的签名，换匿名设备也无法恢复。
- 将 `openhongguo` 从 0.4.0 升级至 0.4.1；版本升级并不会自动证明实机播放已恢复，需等 CI 构建并由用户实测。
- 播放器失败界面增加重新尝试按钮；若诊断码支持匿名设备刷新，则显示“更换匿名设备重试”。
- 设备接力配置存入 `tv_handoff_identity` 的 `enabled` 字段，首次安装默认 `true`；设置页和设备接力页共用一个开关，关闭时取消 NSD 广播且不接收 TCP 播放任务。应用进入后台时仍停止广播，以减少后台活动。
- app 版本提高为 0.1.1，CI Artifact 名与 SHA 文件同步更新。正式 Release 只在本次 Android CI 成功后由 `.github/workflows/publish-v0.1.1.yml` 发布。

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


## 2026-11-24：Lint opt-in false positives

- 第 14 次 Actions 中单测全部通过；Lint 汇总为 96 errors、26 warnings、1 hint。首个错误是 `UnsafeOptInUsageError`，指向迁移自开心果且已使用文件级 `@file:OptIn(UnstableApi::class)` 的 Media3 源码。
- 开心果原工程本身已在其 `app/build.gradle.kts` 针对 `UnsafeOptInUsageError` 配置了 Lint disable；新版对齐这一已验证设置，不关闭整个 Lint，Kotlin 编译器仍会检查实际的 opt-in 使用。
- CI 的 Lint 步骤现在会在任务结束后打印完整文本报告，以便若仍有其他问题可以按 issue 和文件逐项修复。此次修复尚待下一轮 Actions 验证。


## 2026-11-24：首轮独立 Lint 与签名容错修正

- 第 14 次 Actions：`testDebugUnitTest` 已通过；`lintDebug` 因 96 个错误失败，第一项定位到迁移的 Media3 `ExoVideo.kt`。现有 Kotlin `@file:OptIn` 能满足编译器，但未被 Android Lint 当作 AndroidX 的 opt-in 证明，因此给实际使用 Media3 的类/函数补充 `androidx.annotation.OptIn(markerClass = [UnstableApi::class])`；后续 CI 将确认是否覆盖所有问题。
- 调整 Gradle 签名防护：只有显式请求 Release APK 打包、且根目录正式签名配置不完整时才失败；本地部分密钥配置不会再阻止 Debug 构建、测试与 Lint。签名属性文件解析错误会给出明确中文错误，不输出秘密值。
- 本次代码修正已提交；Lint、APK 打包、ABI 及 SHA 仍以新的 Actions 结果为准。


## 2026-11-24：Universal APK 的 ABI 验证方式修正

- 最新 Actions #16 的 `testDebugUnitTest` 通过，Android Lint 通过（0 errors，26 warnings，1 hint），`assembleDebug` 通过并生成 `app/build/outputs/apk/debug/app-universal-debug.apk`。
- ABI 检查脚本此前错误地要求三个独立 ABI 子目录 APK，导致脚本失败，未能进入 checksum/artifact 上传。Gradle 此次实际只产出 Universal APK，因此检查改为读取 APK ZIP 内的真实路径，要求三个 ABI 的 .so 文件均存在，并分别要求 openhongguo 的 `libgojni.so`。这比仅依据 split 配置或 AAR 的理论能力判断可靠。
- 参考的 `openhongguo:0.4.0` AAR 缓存中可见 `libgojni.so` 的 x86_64、arm64-v8a、armeabi-v7a、x86 构建；Lint 还警告 arm64-v8a 的 libgojni.so 未按 16 KB 页面对齐。ABI 是否装入 Universal APK 将由下一轮 CI 明确验证；16 KB 对齐警告保留记录，不伪装已解决。
- 因 ABI 检查失败，本轮未上传 APK artifact；需要通过新的 workflow 后才能发布 SHA-256 和下载链接。

 
## 2026-11-24：修复 ABI 校验脚本 YAML 截断

- 提交 #17 在 Actions 中立即失败且没有 job/check-run；检查发现上一轮生成 workflow 时，ABI 断言行在 `/libgojni.so` 处被截断，造成无效 YAML。这不是应用源码编译失败。
- 已恢复完整的 grep 断言、for 循环收尾及 success 日志。下一轮 Actions 需验证 workflow 被正确解析，并实际检查 Universal APK 内的三种 ABI。

 
## 2026-11-24：恢复已知可解析的 Actions 工作流基线

- 新的 ABI 检查修订 #17/#18 以零 job、零 check-run 在创建后立即失败，表明 Workflow 在 runner 分配前未被正常加载。为避免继续局部编辑一个可能已损坏的 YAML 文件，恢复到已被 #16 实际执行、单测/Lint/assembleDebug 都成功的 workflow 基线，然后只替换 ABI 检查区块。
- 新块直接检查 Universal APK 的 ZIP 条目，验证三个 ABI 的本地库和每个 ABI 的 `libgojni.so`；其余测试、Lint、签名和 Artifact 步骤沿用已执行过的工作流。
- 下一次 CI 的首要验证是 workflow 正常生成 job；仍需确认 ABI 检查与 Artifact 上传成功。


## 2026-11-24：按项目要求在仓库根目录补齐实际签名文件

- 已在新版仓库根目录生成并加入独立的 `hongguoTV-next-release.jks` 与 `signing-secrets.txt`，不再只提供 Gradle 读取逻辑或要求用户另行创建。
- JKS 是新生成的独立 RSA 发布证书（alias `hongguotv-next-release`），不复制开心果或旧 TV 的密钥。
- GitHub Actions 签名步骤优先使用这两个根目录文件；只有两者确实不存在时才退回 Actions Secrets 流程。自用项目的正式签名构建不再依赖额外配置 Secrets。
- 工作流上传 APK、校验文件与证书信息，不将 JKS/属性文件作为 Artifact；旧的两个参考仓库未修改。
- 正式签名首个 CI 构建与最终 SHA-256 需要由新一次 Actions 确认。


## 2026-11-24：根目录正式签名与 CI 交付验证完成

- 按要求把独立的 `hongguoTV-next-release.jks` 和 `signing-secrets.txt` 实际放到了仓库根目录；Gradle 从 `rootProject.file(...)` 读取。CI 优先使用根目录这两份文件，不再要求配置 Actions Secrets。该提交为 `9317ca40ae8c08dae533f5ccb6c3a4e7379eec46`。
- Actions #20：单元测试成功；Android Lint 成功（保留 26 条 warning 和 1 条 hint，没有 error）；Debug APK 成功构建；Universal APK 实际包含 `x86_64`、`arm64-v8a`、`armeabi-v7a` 三种 native library，三种 ABI 均发现 `libgojni.so`。
- 正式签名 Release 构建、`apksigner verify` 和证书/SHA 产物生成均成功；签名证书 SHA-256 指纹：`ebf27c606826ad1ae5ac72a2ae31751b2769c2c0029b3ac4d1e748d3daa57a34`。
- 正式签名 APK：`hongguoTV-next-v0.1.0-universal-release-signed.apk`；SHA-256：`11145b0b612765dfaef40b57e191d54a63423071058c8a393c6b43e22b3c9dbb`。SHA 在 runner 和下载到本地后均已校验通过。
- 构建记录：[Actions #20](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45346930533)。正式签名 artifact 名称：`hongguoTV-next-v0.1.0-formal-release`；测试签名 artifact 名称：`hongguoTV-next-v0.1.0-test-apk`。
- 仍未通过真机验证：电视实机安装/播放、遥控器焦点行为、实际手机到电视的接力通信。Lint 还提示 `openhongguo 0.4.0` 的 ARM64 `libgojni.so` 未满足 16 KB 页面对齐；当前没有声称解决该项。
