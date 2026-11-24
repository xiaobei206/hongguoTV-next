# 红果 TV Next

独立的新一代 Android TV 客户端。所有新版开发仅写入本仓库；旧版 [hongguoTV](https://github.com/xiaobei206/hongguoTV) 保持只读，手机/平板项目 [kaixinguo-android](https://github.com/xiaobei206/kaixinguo-android) 是核心架构参考。

## 系统兼容目标

- 使用一个 Universal APK，不拆分 Legacy / Standard 两套业务代码。
- 统一安装最低版本为 Android 6.0（API 23），当前版本将 minSdk 设为 23。
- 启用 Core Library Desugaring，让睡眠定时器和诊断时间戳使用的 java.time.Instant 可在 API 23 上运行。
- API 23-27 使用 BitmapFactory 封面解码与 JPEG/PNG/WebP 请求回退，使用同目录 File.renameTo 原子保存历史/收藏，NSD 发现集合不依赖 API 24 KeySetView。
- Android CI 检查最终 APK Manifest，硬性要求 sdkVersion 为 23；依赖或 Manifest 合并若抬高最低系统版本，构建将失败。
- API 23 代表安装与基础运行兼容，不代表老电视硬件一定支持所有 DRM、编码格式、清晰度或硬件解码能力；这些仍需实机验证。

## 当前实现

- Kotlin + Jetpack Compose 的电视端主流程：推荐、排行榜、新剧上线、搜索、收藏、历史、详情、选集、播放器、设备接力、诊断和设置入口。
- 迁移开心果的 AppModel、Catalog、Card、Feed、收藏/历史持久化、会话状态、播放状态机、CoverStore、Media3 ExoPlayer、流注册与 CENC 解密、播放诊断。
- 迁移旧 TV 的 TCP/NSD 投屏接力服务及发往开心果手机端的诊断客户端；接力协议保持版本 1，独立协议编解码器有单元测试。设备接力开关持久保存，默认开启，关闭后不广播 NSD 并拒收接力任务。
- 页面首批数据先呈现，Feed 独立分页；接近末尾续载，补载目标约两屏；播放开始时取消 Feed 请求与待处理封面下载。
- 独立 applicationId：`com.xiaobei206.hongguotvnext`，用于与旧版本并行安装。

## 当前验证状态

当前开发版本为 v0.1.4。统一安装包的最低系统要求已设为 Android 6.0（API 23），并加入 Core Library Desugaring、旧版封面解码与文件/NSD 兼容实现。正式发布由带 release 标记的提交对应的 Android CI 全部通过后自动完成；老电视实机安装/播放仍需单独验收。本版修复此前只升级官方 Maven SDK、却未接入开心果空响应恢复实现的问题：Android CI 固定检出自维护的 `openhongguo` 提交 `1e9334185a46033bcc4e3e30fc41f649958b1389`，运行 SDK Go 测试、应用诊断补丁、编译本地 AAR，再将该 AAR 注入 NEXT。V5 POST 返回 HTTP 200 空响应后，SDK 会尝试同一身份的签名、无请求体 `video_tab` GET，并严格校验当前分集；APK 打包没有指定该 AAR 会直接失败。

v0.1.3 的播放空响应修复已通过 Android CI 并正式发布；SDK 空响应回退的目标电视实播仍需在设备上验证。不能把 CI 构建成功等同于特定电视的硬件解码或播放成功。

v0.1.3 已正式发布：[GitHub Release 下载页](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.3)。该版本的单元测试、Android Lint、Debug 构建、Universal APK 三种 ABI 检查、正式签名 APK 构建和签名证书/SHA-256 校验均已通过。电视遥控器和实际播放仍需在目标电视上验收。

v0.1.4 最终发布标记提交：`release: prepare v0.1.4 final`。正式 Release 仅由该提交对应的成功 Android CI 自动发布；发布工作流还会校验标记提交和 main 最新提交一致。

v0.1.0 的正式签名 APK SHA-256：`11145b0b612765dfaef40b57e191d54a63423071058c8a393c6b43e22b3c9dbb`。签名证书指纹：`ebf27c606826ad1ae5ac72a2ae31751b2769c2c0029b3ac4d1e748d3daa57a34`。

尚未声称已完成 x86_64 模拟器安装、ARM 电视实机播放、遥控器长时间焦点验证、真实手机接力或特定电视硬件解码测试；这些仍需设备验证。

## 构建

需先编译固定版本的自维护 SDK。CI 按相同顺序执行；APK 打包不允许不带该 AAR。

```bash
mkdir -p .build
git clone https://github.com/xiaobei206/openhongguo .build/openhongguo
git -C .build/openhongguo checkout 1e9334185a46033bcc4e3e30fc41f649958b1389
python3 scripts/patch-openhongguo.py .build/openhongguo
(cd .build/openhongguo && go test ./...)
(cd .build/openhongguo/bindings/mobile/core && go test ./...)
go install golang.org/x/mobile/cmd/gomobile@v0.0.0-20261011034528-197770444867
export PATH="$(go env GOPATH)/bin:$PATH"
gomobile init
(cd .build/openhongguo/bindings/mobile && ./build.sh android)
./gradlew testDebugUnitTest lintDebug assembleDebug \
  -PpatchedOpenhongguoAar="$PWD/.build/openhongguo/bindings/mobile/android/build/outputs/aar/openhongguo-release.aar"
```

Windows 用户可在已配置相同 Go/NDK 环境的 WSL 中执行 SDK 构建；CI 使用相同脚本和 AAR 注入参数。

CI 将单元测试、Android Lint、Debug 构建分成独立步骤，并使用 Gradle 缓存；Debug APK 使用 Android Debug 测试证书，通过 Workflow Artifact 提供下载。测试签名 APK 不等于正式发布签名，也不保证能够覆盖安装未来正式签名版本。

若在 GitHub Actions Secrets 配齐四项正式签名 Secret，CI 还会从仓库根目录安全还原独立发布 JKS，构建并验证正式签名 Release APK，输出证书指纹与 SHA-256，并在结束后删除临时密钥文件。四项全部未配置时只生成测试签名版本；只配置其中一部分会明确失败。

## 播放 SDK 来源

- 固定源码：`xiaobei206/openhongguo@1e9334185a46033bcc4e3e30fc41f649958b1389`。
- `scripts/patch-openhongguo.py` 用于暴露不含敏感参数的 HTTP 状态、响应长度与接口路径，并保留当前 SDK 的播放回退逻辑。
- CI 先运行 SDK 与 mobile core 的 Go 测试，再编译本地 AAR；不会把官方 Maven AAR 用作正式打包输入。

## ABI

Gradle 配置了 ABI 拆分与 Universal APK。当前 CI 实际输出为一个 Universal Debug APK，而不是三个独立 ABI 子目录 APK；因此 CI 会直接检查 Universal APK 内容，要求 `x86_64`、`arm64-v8a`、`armeabi-v7a` 均含本地库，并逐一确认 `openhongguo` 的 `libgojni.so` 存在。最终兼容性仍以 APK 内容和模拟器/真机安装结果为准。

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

本自用项目已按要求将 `hongguoTV-next-release.jks` 与 `signing-secrets.txt` 实际放在仓库根目录，CI 直接读取这两份文件生成正式签名 APK。其他本地密钥文件仍由 `.gitignore` 忽略；构建 Artifact 只包含 APK、SHA-256 与证书信息，不包含密钥文件本身。

根目录签名文件已实际存在，日常 CI 无需额外配置 Actions Secrets。若以后想改用 Secrets 签名，Workflow 仍保留 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD` 的备用读取逻辑。

## 官方参考

- [Compose for TV](https://developer.android.com/training/tv/playback/compose)
- [TV 滚动列表与焦点行为](https://developer.android.com/training/tv/playback/compose/lists)
- [Media3 播放器与 MediaSessionService](https://developer.android.com/media/implement/playback-app)

## 项目文档

- [累计开发日志](DEVELOPMENT-LOG-CN.md)
- [版本变更](CHANGELOG.md)
- [交接索引](HANDOFF-CN.md)
- [签名说明](SIGNING-CN.md)
