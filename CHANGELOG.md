# Changelog

## v0.1.0 — 初始化与核心迁移（开发中，未发布）

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
- GitHub Release 页面尚未创建。CI 成功不等于真机播放、遥控器和手机接力已验证。
