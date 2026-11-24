# 签名配置说明

## 本地配置（所有文件均在仓库根目录）

- 密钥文件：`hongguoTV-next-release.jks`
- 属性文件：`signing-secrets.txt`
- 属性键名：`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`

两个文件都已被根目录 `.gitignore` 忽略。不要把密钥文件、属性文件或其中的内容复制到 README、提交说明、日志或普通构建 Artifact。

配置缺失时，`assembleDebug` 仍可生成测试 APK。若只提供了两个文件中的一个，或属性键缺失，Gradle 会直接报错。缺少密钥时 `assembleRelease` 不会用 Debug 证书冒充正式签名。

## GitHub Actions Secrets

需要进行正式发布签名时，在仓库 Settings → Secrets and variables → Actions 设置：

- `ANDROID_KEYSTORE_BASE64`：发布 JKS 文件的 Base64 文本。
- `ANDROID_KEYSTORE_PASSWORD`：JKS 密码。
- `ANDROID_KEY_ALIAS`：密钥别名。
- `ANDROID_KEY_PASSWORD`：密钥密码。

Workflow 应将 JKS 安全还原到仓库根目录，在任务结束后清理；不得上传密钥或属性文件。未配置这些 Secrets 时只生成明确标记的测试签名 APK。新版使用独立 applicationId 与独立签名，不继承或复制旧项目密钥。
