# WebSSH Android

Android 客户端，通过 WebSocket 连接 WebSSH 后端，实现远程服务器的 SSH 终端和 SFTP 文件管理。

## 功能清单

### Phase 1 — 核心功能
- ✅ 登录/登出 + Token 认证
- ✅ 服务器列表展示（卡片式，显示名称、地址、端口）
- ✅ 服务器增删改（AddEditServerScreen）
- ✅ SSH 终端（xterm.js + WebSocket + WebView）
- ✅ SFTP 文件浏览器（目录浏览、面包屑导航、排序）
- ✅ 文件上传（系统文件选择器 → Base64 → API）
- ✅ 单文件下载（保存到系统 Downloads 目录）
- ✅ 文件预览（文本文件弹窗显示）

### Phase 2 — 体验增强
- ✅ 批量选择模式（多选文件 → 打包 ZIP 下载）
- ✅ 设置页面（备份/恢复/修改密码）
- ✅ 标签筛选（FilterChip 按标签过滤服务器）
- ✅ 标签管理（添加/编辑服务器时输入标签）

### Phase 3 — 锦上添花
- ✅ SSH 密钥认证（粘贴私钥 + 可选密码）
- ✅ 文件搜索（按名称实时过滤）
- ✅ 文件权限显示（`rwxr-xr-x` 格式）
- ✅ 暗色主题优化（深蓝黑底 + Android 12 动态取色）

### Bug 修复与优化
- ✅ 系统返回键逻辑（首页弹确认 → 退出，其他页返回上级）
- ✅ 记住用户名密码（DataStore 持久化 + 复选框）
- ✅ SSH 混合内容修复（xterm.js 内嵌 assets，file:// 协议）
- ✅ WebSocket 参数名修复（`serverId` → `server`）
- ✅ SSH 终端虚拟快捷键工具栏（Ctrl/Tab/Esc/方向键/F键）
- ✅ 终端自适应键盘（65% 屏高，工具栏固定在键盘上方）
- ✅ 自定义应用图标

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 网络 | Retrofit 2 + OkHttp 4 |
| 序列化 | Gson |
| 持久化 | DataStore Preferences |
| SSH 终端 | xterm.js (WebView + WebSocket) |
| 构建 | Gradle 8.2 + AGP 8.2.0 |

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── assets/                          # SSH 终端前端资源
│   ├── ssh_terminal.html            # xterm.js 终端页面
│   ├── css/xterm.css                # xterm 样式
│   └── js/                          # xterm.js 库文件
├── java/com/webssh/
│   ├── MainActivity.kt              # 入口 Activity
│   ├── api/
│   │   ├── WebSSHApi.kt             # Retrofit API 接口定义 + 数据模型
│   │   ├── NetworkClient.kt         # OkHttp 客户端 + Auth 拦截器
│   │   └── TokenManager.kt          # DataStore Token/凭据管理
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── MainNavigation.kt    # 路由导航 + 状态管理
│   │   │   ├── LoginScreen.kt       # 登录页（记住密码）
│   │   │   ├── ServerListScreen.kt  # 服务器列表（标签筛选）
│   │   │   ├── AddEditServerScreen.kt # 添加/编辑服务器
│   │   │   ├── FileManagerScreen.kt # SFTP 文件管理（批量/搜索）
│   │   │   ├── SshTerminalScreen.kt # SSH 终端（WebView）
│   │   │   └── SettingsScreen.kt    # 设置（备份/恢复/改密）
│   │   └── theme/Theme.kt           # 主题（亮/暗/动态取色）
│   └── viewmodel/
│       └── WebSSHViewModel.kt       # ViewModel（所有业务逻辑）
└── res/
    ├── mipmap-*/                    # 应用图标
    ├── values/strings.xml           # 字符串资源
    └── values-zh/strings.xml        # 中文本地化
```

## API 对接

所有 API 调用通过 `WebSSHApi` Retrofit 接口，`NetworkClient` 的 `authInterceptor` 自动在每个请求中注入 `Authorization: Bearer {token}`。

### 认证
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/login` | POST | 登录获取 token |
| `/api/logout` | POST | 登出清除会话 |
| `/api/admin/password` | POST | 修改管理员密码 |

### 服务器管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/servers` | GET | 获取服务器列表 |
| `/api/servers` | POST | 添加服务器 |
| `/api/servers/:id` | PUT | 更新服务器 |
| `/api/servers/:id` | DELETE | 删除服务器 |

### SFTP 文件操作
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/sftp/list` | GET | 列出目录 |
| `/api/sftp/upload` | POST | 上传文件 |
| `/api/sftp/download` | GET | 下载单文件 |
| `/api/sftp/download-batch` | POST | 批量下载 ZIP |
| `/api/sftp/read` | GET | 预览文件内容 |
| `/api/sftp/mkdir` | POST | 新建文件夹 |
| `/api/sftp/rename` | POST | 重命名 |
| `/api/sftp/delete` | POST | 删除 |

### 数据管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/backup` | GET | 备份服务器配置 |
| `/api/admin/restore` | POST | 恢复服务器配置 |

### WebSocket
| 端点 | 协议 | 说明 |
|------|------|------|
| `/ws/ssh?server={id}&token={token}` | WebSocket | SSH 终端连接 |

## SSH 终端实现

### 架构
```
Android App (SshTerminalScreen.kt)
  └─ WebView
       └─ ssh_terminal.html (本地 assets)
            ├─ xterm.js (终端渲染)
            ├─ xterm-addon-fit (自动适配尺寸)
            └─ WebSocket → 后端 → SSH2 → 远程服务器
```

### 关键设计决策

1. **本地 assets 加载 xterm.js**：避免 HTTPS CDN → ws:// 的混合内容安全错误
2. **WebSocket URL 注入**：Kotlin 读取 HTML 模板 → 替换 `%WS_URL%` 占位符 → loadDataWithBaseURL
3. **终端高度**：固定 65% 屏幕高度，工具栏固定在 65% 位置
4. **虚拟快捷键**：解决手机键盘缺少 Ctrl/Tab/Esc/F键的问题
5. **auto-scroll**：重写 `term.write()`，每次输出后自动滚动到底部

## 构建

```bash
# 前置条件
export ANDROID_SDK_ROOT=/path/to/android-sdk

# 构建 Debug APK
./gradlew assembleDebug

# 输出
app/build/outputs/apk/debug/app-debug.apk
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-03-28 | 基础 SSH + SFTP 功能 |
| 1.1.0 | 2026-03-30 | 批量下载、设置、标签筛选 |
| 1.2.0 | 2026-03-30 | 密钥认证、搜索、权限显示、暗色主题 |
| 1.2.1 | 2026-03-30 | SSH 终端优化（键盘适配、快捷键工具栏） |

---
*文档更新: 2026-03-30*
