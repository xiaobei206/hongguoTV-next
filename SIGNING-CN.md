# 签名配置说明

## 本地配置（所有文件均在仓库根目录）

- 密钥文件：`hongguoTV-next-release.jks`
- 属性文件：`signing-secrets.txt`
- 属性键名：`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`

这两份文件按本项目的自用要求保存在仓库根目录，并由 CI 直接读取；它们不会包含在 APK Artifact 内。

配置缺失或只配置了一部分时，`assembleDebug`、单元测试与 Lint 仍可继续；Gradle 会给出明确警告。显式请求 `assembleRelease` / `bundleRelease` 时，如果根目录 JKS、属性文件或必要参数缺失，Gradle 会报出具体缺项并停止，不会生成一个容易被误认为正式版本的未签名/Debug 签名包。

## GitHub Actions

当前正式签名直接读取仓库根目录文件，不需要配置 GitHub Actions Secrets。只有未来主动改回 Secrets 流程时才需要以下参数：

- `ANDROID_KEYSTORE_BASE64`：发布 JKS 文件的 Base64 文本。
- `ANDROID_KEYSTORE_PASSWORD`：JKS 密码。
- `ANDROID_KEY_ALIAS`：密钥别名。
- `ANDROID_KEY_PASSWORD`：密钥密码。

Workflow 优先使用根目录文件；只有根目录文件缺失时才尝试从 Secrets 还原到根目录。新版使用独立 applicationId 与独立签名，不继承或复制旧项目密钥。
