# 开发日志（持续累积）

## 2026-11-24：仓库审计与基础工程启动

### 仓库审计
- `hongguoTV-next` 是空的公开仓库，默认分支为 `main`；后续所有新版提交只写入该仓库。
- `kaixinguo-android` 与旧 `hongguoTV` 都包含要求检查的核心 Kotlin 文件与测试目录。
- 开心果项目 Gradle version catalog 当前使用 AGP 9.5.1、Kotlin 2.4.21、Compose BOM 2026.09.00、Media3 1.11.1、openhongguo 0.4.0、Gradle Wrapper 9.8.2；新版先以这组已在参考项目中使用的版本为基线，构建结果再决定是否调整。
- 开心果的 `Feed` 已有卡片 ID 去重、独立分页游标、加载状态、失败退避和“无新增内容停止继续翻页”逻辑；`AppModel` 将首页缓存先展示，再由网络结果刷新。后续迁移优先保留这些语义，而非另造一套数据模型。
- 开心果 `CoverStore` 已包含内存 LRU、磁盘 JPEG、URL 任务去重和限流 IO；播放器基于 Media3 ExoPlayer，并通过 `StreamRegistry` / `CencDataSource` 接入 CENC 流。
- 旧 TV 的接力服务通过 NSD 广告 `_hongguotv._tcp.`，TCP JSON 协议版本为 1；请求字段含 `type=play`、`transferId`、`targetDeviceId`、`seriesId`、`episode`、`positionMs`。这将作为兼容实现的协议基准，尚未在新版中验证。
- 两个参考公开仓库当前树中均有根目录 JKS 与签名配置文件。新版不会复制这些凭据，发布密钥必须独立、私有且固定。

### 本阶段修改
- 创建独立工程的基础 Gradle 配置、Compose TV shell、Manifest、中文项目文档和 GitHub Actions 骨架。
- 签名读取路径固定使用 `rootProject.file(...)`；签名输入不完整时明确报错，不以 Debug 签名冒充正式签名。
- 测试构建不要求预先提供正式签名凭据。

### 验证记录
- 仓库与文件审计：已完成部分入口核查。
- 自动构建：待新 workflow 执行。
- Feed、ABI 与投屏协议测试：待迁移后补齐。
- 模拟器及真实电视：尚未运行。
