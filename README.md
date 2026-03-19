# WebSSH Android App

一个美观易用的 Android 文件管理应用，用于管理远程服务器文件。

## 功能特性

- 🔐 安全登录（Token 认证）
- 📋 服务器列表管理
- 📁 文件浏览器（SFTP）
- 📂 文件夹操作（创建、重命名、删除）
- 🔄 排序功能（按名称、大小、时间）
- 🎨 Material You 设计风格
- 🌙 支持深色模式

## 后端要求

需要在服务器上运行 WebSSH 后端服务：
- 部署地址：`http://192.168.100.20:3000`
- 支持的 API 版本：v1.0.0+

## 安装方法

### 方式一：使用 Android Studio

1. 克隆或下载此项目
2. 使用 Android Studio 打开项目
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮

### 方式二：使用命令行

```bash
# 生成 APK
cd webssh-android
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

1. **首次使用**
   - 打开 APP
   - 输入服务器地址（默认：`http://192.168.100.20:3000`）
   - 输入用户名和密码
   - 点击登录

2. **服务器列表**
   - 显示所有已配置的服务器
   - 点击服务器进入文件管理

3. **文件管理**
   - 浏览目录
   - 新建文件夹
   - 重命名文件/文件夹
   - 删除文件/文件夹
   - 排序方式切换

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构**：MVVM
- **网络请求**：Retrofit 2 + OkHttp
- **本地存储**：Jetpack DataStore
- **异步处理**：Kotlin Coroutines

## 项目结构

```
app/src/main/java/com/webssh/
├── MainActivity.kt              # 主 Activity
├── api/                        # API 接口层
│   ├── WebSSHApi.kt           # API 定义
│   ├── TokenManager.kt        # Token 管理
│   └── NetworkClient.kt       # 网络客户端
├── viewmodel/                  # ViewModel 层
│   └── WebSSHViewModel.kt     # 主 ViewModel
├── ui/                         # UI 层
│   ├── screens/               # 页面
│   │   ├── LoginScreen.kt
│   │   ├── ServerListScreen.kt
│   │   ├── FileManagerScreen.kt
│   │   └── MainNavigation.kt
│   └── theme/                 # 主题
│       └── Theme.kt
```

## 最低要求

- Android 7.0 (API 24) 或更高版本

## 注意事项

- 确保设备和服务器在同一网络或已配置端口号映射
- 后端需要配置 `allowCleartextTraffic` 允许 HTTP 请求
- Token 有效期为 30 分钟

## 许可证

MIT License

## 作者

WebSSH Team
