# WebSSH Android

Android 客户端，通过 WebSocket 连接 WebSSH 后端，实现远程服务器的 SSH 终端和 SFTP 文件管理。

## 功能概览

### 🔐 认证与安全
- 用户名/密码登录（支持记住密码）
- 指纹生物识别登录（设置中启用）
- Token 自动管理（拦截器注入）

### 🖥️ SSH 终端
- xterm.js + WebSocket 终端（WebView 渲染）
- 虚拟快捷键工具栏（顶部固定）：Ctrl、Tab、Esc、方向键、Home/End、PgUp/PnDn、F1-F12
- 终端区域 45% 屏幕高度，键盘弹出不遮挡内容
- 自动滚动到光标位置

### 📁 SFTP 文件管理
- 目录浏览与面包屑导航
- 文件上传（系统选择器 → Base64）
- 单文件下载（保存到 Downloads）
- 批量选择 → ZIP 打包下载
- 文本文件预览
- 新建文件夹 / 重命名 / 删除
- 文件搜索（按名称过滤）
- 文件权限显示（rwx 格式）
- 按名称/大小/时间/权限排序

### ⚙️ 服务器管理
- 服务器增删改（卡片式 UI）
- 密码认证 + SSH 密钥认证（粘贴私钥）
- 标签管理与筛选（FilterChip）
- 启用/禁用开关

### 📦 数据管理
- 备份：导出服务器配置 JSON（支持分享/复制）
- 恢复：粘贴 JSON 导入
- 修改管理员密码

### 🎨 界面
- Material 3 设计语言
- 暗色主题（Android 12+ 动态取色）
- 自定义应用图标（Adaptive Icon）
- 中文本地化

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 网络 | Retrofit 2 + OkHttp 4 + Gson |
| 持久化 | DataStore Preferences |
| 生物识别 | androidx.biometric |
| SSH 终端 | xterm.js (WebView + WebSocket) |
| 构建 | Gradle 8.2 + AGP 8.2.0 |

## 项目结构

```
app/src/main/
├── assets/                              # SSH 终端前端
│   ├── ssh_terminal.html                # xterm.js + 快捷键工具栏
│   ├── css/xterm.css
│   └── js/                              # xterm.js 库
├── java/com/webssh/
│   ├── MainActivity.kt                  # FragmentActivity 入口
│   ├── api/
│   │   ├── WebSSHApi.kt                 # Retrofit API + 数据模型
│   │   ├── NetworkClient.kt             # OkHttp + Auth 拦截器
│   │   └── TokenManager.kt             # DataStore（Token/凭据/指纹开关）
│   ├── ui/screens/
│   │   ├── MainNavigation.kt           # 路由 + 状态 + 生物识别
│   │   ├── LoginScreen.kt              # 登录（记住密码 + 指纹按钮）
│   │   ├── ServerListScreen.kt         # 服务器列表（标签筛选）
│   │   ├── AddEditServerScreen.kt      # 添加/编辑（密码/密钥）
│   │   ├── FileManagerScreen.kt        # SFTP（批量/搜索/权限）
│   │   ├── SshTerminalScreen.kt        # SSH（WebView）
│   │   └── SettingsScreen.kt           # 设置（备份/恢复/指纹/改密）
│   ├── ui/theme/Theme.kt               # 主题
│   └── viewmodel/WebSSHViewModel.kt    # 业务逻辑
└── res/
    ├── mipmap-anydpi-v26/              # Adaptive Icon
    ├── mipmap-*/                       # 各密度图标
    └── values/                         # 字符串、主题
```

## 构建

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-03-28 | 基础功能：登录、服务器管理、SSH终端、SFTP |
| 1.1.0 | 2026-03-30 | 批量下载 ZIP、设置页、标签筛选 |
| 1.2.0 | 2026-03-30 | 密钥认证、文件搜索、权限显示、暗色主题 |
| 1.2.1 | 2026-03-30 | SSH 终端优化：键盘适配、快捷键工具栏 |
| 1.3.0 | 2026-03-30 | 指纹生物识别登录、自定义应用图标 |

## 相关项目

- [WebSSH 后端](../webssh) — Node.js + Express + ssh2 后端服务
- [API 文档](../webssh/API.md) — 后端 API 完整说明

---
*最后更新: 2026-03-30*
