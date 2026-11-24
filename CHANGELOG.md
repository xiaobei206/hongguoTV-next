# Changelog

## v0.1.4 — Android 6.0 / API 23 统一兼容（最终发布候选；CI 与实机验收分开）

- 将唯一 Universal APK 的 minSdk 从 API 28 降至 API 23，目标覆盖 Android 6.0 及以上电视；不拆分 Legacy / Standard 两套代码。
- 启用 Core Library Desugaring（desugar_jdk_libs 2.1.5），使睡眠定时器与手机诊断使用的 java.time.Instant 可在 API 23 上运行。
- 为 API 23-27 增加 BitmapFactory 封面解码及常见图片格式协商回退；历史/收藏文件使用同目录 File.renameTo 原子替换；电视发现服务的 resolving 集合改用 API 23 可用的同步集合。
- Android CI 新增对实际生成 APK Manifest 最低 SDK 的断言，要求 sdkVersion 为 23；如传递依赖要求更高 API 或 Manifest 合并抬高门槛，构建将明确失败。
- 保持现有 Media3 1.11.1、自维护 openhongguo AAR、ABI 与播放逻辑，不为降低安装门槛而替换播放器。
- API 23 的安装兼容性不等同于 DRM、编码器或硬件解码能力；小米 E55A、东芝 75U6900C 仍需分别实机安装和播放验收。

## v0.1.3 — 播放空响应恢复与 SDK 构建一致性（待 CI / 实机验收）

- 修复只升级官方 Maven SDK、却漏掉开心果已完成的空响应恢复逻辑的问题。
- Android CI 固定检出 `xiaobei206/openhongguo@1e9334185a46033bcc4e3e30fc41f649958b1389`，运行 Go 与 mobile core 测试、应用诊断补丁并编译本地 AAR；应用 APK 明确使用该 AAR。
- V5 播放 POST 返回 HTTP 200 空响应时，优先使用同一匿名设备身份发起签名、无请求体的 `video_tab` GET；保留当前分集校验与旧接口兼容回退。
- 没有指定自维护 AAR 时，Gradle 拒绝执行 APK 打包任务，避免再次发布未包含修复的包。
- 增强 SDK 构建期诊断；只记录去 query 的接口路径、状态码、响应字节数和脱敏设备指纹，不记录原始设备标识、Cookie、Token、签名或完整响应体。
- 本版本的 CI 结果和目标电视实机播放均需分别验证。

## v0.1.2 — 电视搜索与播放遥控体验（已正式发布）
## v0.1.2 — 电视搜索与播放遥控体验（已正式发布）

- 搜索页在输入框为空时显示开心果原有的分组热搜榜，保留排名、封面、热度/搜索指数和集数信息；支持遥控器焦点选中后进入详情。
- 新剧上线日期按钮使用开心果的日期标签规则：今天、明天、昨天或“月日”，不再并排显示完整年月日数字；筛选仍使用原始日期值。
- 播放页按电视遥控器习惯处理：隐藏菜单时左右快退/快进 10 秒、上下切换剧集；OK 唤出菜单，菜单显示时再按 OK 执行播放/暂停；菜单超时后不保留控制按钮。
- 菜单首次唤出后无操作约 3 秒隐藏；操作控制按钮后约 5 秒隐藏；切集后约 0.5 秒隐藏；快进/快退提示短暂显示。
- 在播放菜单增加中文“播放诊断”开关；左上角英文 STATS FOR NERDS 悬浮面板显示视频/音频编码、解码器、分辨率、帧率、码率、网络状态、丢帧数和播放位置。诊断面板独立于菜单显示。
- 返回键采用两步退出保护；失败状态保留重试入口。真实遥控器焦点、按键重复和视频解码仍需电视实测。
- 正式发布：[v0.1.2 Release](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.2)。Android CI 的单元测试、Lint、Debug APK、三种 ABI 检查、正式签名 APK 和证书/SHA-256 校验全部通过。

## v0.1.1 — 播放兼容性与接力设置（已正式发布）

- 升级 openhongguo 到 0.4.1，修复 0.4.0 在 2026-11-23 起因签名设备环境值不匹配导致的播放模型 HTTP 200 空响应。
- 增加可持久保存的“设备接力（手机投屏）”开关，新安装默认开启；关闭后不再广播 NSD 发现服务，也不接收接力任务。
- 在播放失败界面提供重试入口；检测到匿名设备受限时，可直接刷新匿名设备并重试。

## v0.1.0 — 初始化与核心迁移（已正式发布）

### 已提交的实现
- 创建独立 Android TV 工程与 `com.xiaobei206.hongguotvnext` applicationId。
- 加入 Compose TV 主导航与页面：推荐、排行榜、新剧、搜索、收藏、历史、详情/选集、播放、接力、诊断和设置。
- 迁移开心果的数据模型、Feed 分页、持久化存储、播放状态机、封面缓存、Media3、CENC、播放诊断及核心单测。
- 迁移旧 TV 的接力服务与手机诊断发送端，保留 `_hongguotv._tcp.`、协议版本 1 和请求/响应字段，并新增协议单元测试。
- 加入播放时取消待处理 Feed 请求与封面下载的基础资源隔离。
- 配置根目录发布签名读取、私密文件忽略规则和 GitHub Actions 构建工作流。
- 为 Media3 使用位置补充 AndroidX Lint 可识别的 opt-in 标注；修正不完整签名配置不阻断 Debug 开发，但拒绝无正式签名的 Release 打包。该修复仍待 CI 复验。
- CI 测试、Lint 拆成独立步骤并加入 Gradle 缓存。
- 按自用项目要求，在仓库根目录实际加入独立 `hongguoTV-next-release.jks` 与 `signing-secrets.txt`，并令 CI 优先使用根目录这两份文件产生正式签名 Release Artifact。
- CI 要求 Universal APK 实际包含 x86_64、arm64-v8a、armeabi-v7a 三种 ABI 的 native libraries，且每种 ABI 均包含 openhongguo 的 libgojni.so。

### 验证状态
- CI #20 已通过单元测试、Lint、Debug 构建、ABI 原生库检查、正式签名 Release 构建及证书/SHA 验证；产物见 [Actions #20](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45346930533)。
- v0.1.0 已有正式 GitHub Release：[查看发布页](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.0)。CI 成功不等于真机播放、遥控器和手机接力已验证。
