# 独立构建

## 环境

- Python 3.9+，只使用标准库，不需要 pip。
- JDK 17（包含 java、javac、jar、keytool）。
- Android SDK Platform 35 与 Build Tools 35.0.0。

可从 Android Studio 的 SDK Manager 安装对应 SDK 组件，或用已安装的 sdkmanager：

```powershell
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

本项目使用 javac / D8 / aapt2 构建，没有 Gradle wrapper；不能直接作为 Gradle 工程 Sync。APK 目标为 Android 9/API28，使用 API35 只是编译工具配置。

## Windows PowerShell

进入仓库根目录，将路径替换为自己的安装位置：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
py -3 build.py
```

如果 Python 安装没有 `py` 命令，可使用 `python build.py`。也可显式指定：

```powershell
python build.py --java-home 'C:\path\to\jdk-17' --sdk 'C:\path\to\Android\Sdk'
```

非标准 SDK 布局可用 `--build-tools` 和 `--android-jar` 指定文件所在位置。运行 `python build.py --help` 查看全部参数。

Linux/macOS 使用相同脚本，先设置 JAVA_HOME、ANDROID_HOME，再执行 `python3 build.py`。

## 输出与签名

- `dist/USB-Instrument-Probe-0.3.2-local.apk`：本机编译包。
- `dist/SHA256SUMS.txt`：本机包校验值。
- `build/local-debug.keystore`：首次构建生成的本机测试签名；已被 gitignore 排除。

本机测试密钥的标准调试密码在构建脚本中公开，用于可重复的本地构建流程，不是正式签名凭据。原始 0.3.2 发布 APK 的私钥没有提供。本机生成包与原包签名不同，不能直接覆盖安装原包；先用原 App 恢复服务、导出日志并卸载后，才测试本机包。不要在启用代理时直接卸载。

不能承诺逐字节相同 APK：签名、JAR/ZIP 时间戳和素材来源元数据都可能不同。对应关系以原应用源码、Manifest、接口表、DEX 对比和原 APK SHA-256 记录为依据。

## 校验边界

脚本检查 Contracts.java 与 JSON 快照逐项一致、五首 MIDI 校验值，完成 Java 编译、D8、资源打包、对齐、签名并验证 API28 签名。

公开构建不读取 OEM 文件。协议快照包含六组 115 项接口编号及 15 个 MediaData 字段的顺序。接口表变更需要针对目标固件重新核对，不能仅修改某个交易编号猜测兼容。
