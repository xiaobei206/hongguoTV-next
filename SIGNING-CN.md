# 签名配置说明

## 本地配置（所有文件均在仓库根目录）

- 密钥文件：`hongguoTV-next-release.jks`
- 属性文件：`signing-secrets.txt`
- 属性键名：`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`

两个文件都已被根目录 `.gitignore` 忽略。不要把密钥文件、属性文件或其中的内容复制到 README、提交说明、日志或普通构建 Artifact。

配置缺失或只配置了一部分时，`assembleDebug`、单元测试与 Lint 仍可继续；Gradle 会给出明确警告。显式请求 `assembleRelease` / `bundleRelease` 时，如果根目录 JKS、属性文件或必要参数缺失，Gradle 会报出具体缺项并停止，不会生成一个容易被误认为正式版本的未签名/Debug 签名包。

## GitHub Actions Secrets

需要进行正式发布签名时，在仓库 Settings → Secrets and variables → Actions 设置：

- `ANDROID_KEYSTORE_BASE64`：发布 JKS 文件的 Base64 文本。
- `ANDROID_KEYSTORE_PASSWORD`：JKS 密码。
- `ANDROID_KEY_ALIAS`：密钥别名。
- `ANDROID_KEY_PASSWORD`：密钥密码。

Workflow 应将 JKS 安全还原到仓库根目录，在任务结束后清理；不得上传密钥或属性文件。未配置这些 Secrets 时只生成明确标记的测试签名 APK。新版使用独立 applicationId 与独立签名，不继承或复制旧项目密钥。
