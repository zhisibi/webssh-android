# WebSSH Android 开发经验总结

## 项目历程

从零开始，3 个阶段迭代开发，经历了多次调试和修复，最终完成了一个功能完整的 Android SSH 客户端。

## 关键经验

### 1. WebView + WebSocket + xterm.js 的坑

**混合内容安全策略**：Android WebView 加载 HTTPS 页面时，浏览器禁止发起 `ws://` WebSocket 连接。
- ❌ 初版从 CDN 加载 xterm.js（HTTPS），导致 `ws://` 被拦截
- ✅ 解决：将 xterm.js 下载到 `assets/` 目录，通过 `file://` 协议加载

**WebSocket URL 参数对齐**：后端读 `?server=` 但前端发 `?serverId=`，导致服务端拿不到服务器 ID。
- 教训：前后端联调时，参数名必须严格一致。写好 API 文档很重要。

**loadDataWithBaseURL 的 origin**：第一个参数决定了页面的 origin，直接影响 WebSocket 的同源策略。

### 2. Android WebView 的键盘问题

这是整个项目最棘手的问题。手机输入法弹出后会遮挡终端内容。

**尝试过但不够好的方案**：
- `adjustResize` + `visualViewport.resize` 动态调整高度 → 体验不稳定
- `65vh` 固定高度 → 用的是原始视口高度，键盘弹出后比例不对
- `visualViewport.height - toolbar` 动态高度 → 终端内容频繁跳动

**最终方案**：固定比例布局
- 终端：`height: 65vh`（屏幕高度的 65%，固定不变）
- 工具栏：`position: fixed; top: 65vh`（固定在 65% 位置）
- 键盘弹出时，终端区域不受影响，工具栏始终在键盘上方

**教训**：移动端 Web 终端的键盘适配是业界难题。Termius、JuiceSSH 等专业 App 用的是原生终端渲染而非 WebView。Web 方案能用但有局限性。

### 3. 后端 WebSocket 错误处理

Node.js 的 EventEmitter 在没有 error listener 时会直接抛异常导致进程退出。

**崩溃场景**：
1. SSH 认证失败 → ssh2 Client 发出 `error` 事件
2. `ws.send()` 尝试发送错误消息 → WebSocket 已关闭 → 抛异常
3. 异常未捕获 → `Unhandled 'error' event` → 进程崩溃

**解决方案**：
```javascript
// 1. WebSocket 全局错误兜底
ws.on('error', (err) => { console.error('[WS] error:', err.message); });

// 2. 安全发送包装
function safeSend(data) {
  try { if (ws.readyState === WebSocket.OPEN) ws.send(data); } catch(e) {}
}

// 3. conn.on('error') 中使用 safeSend
conn.on('error', (err) => {
  safeSend(JSON.stringify({ type: 'error', data: err.message }));
  try { ws.close(); } catch(e) {}
});
```

### 4. 前后端密码存储的坑

后端对服务器密码做了 AES-256-GCM 加密。Android 端通过 API 添加服务器时：
- 添加：后端自动加密存储 → ✅ 正常
- 更新：如果前端发送空密码 → 后端覆盖为空 → SSH 认证失败 → 崩溃

**教训**：更新操作应该保留旧密码，只在用户明确修改时才覆盖。

### 5. Jetpack Compose 的 StateFlow 驱动

整个 UI 采用单 ViewModel + 多个 StateFlow 的架构：
- `loginState` → 登录/登出状态驱动导航
- `servers` / `files` → 列表数据
- `batchMode` / `selectedFiles` → 批量选择状态
- `toastMessage` → 一次性消息通知

**优点**：状态集中管理，UI 自动响应变化
**注意**：`LaunchedEffect` 监听状态变化做导航时，初始值会触发一次 → 用 `when` 显式处理各状态

### 6. 认证 Header 的重复注入

`NetworkClient` 的 OkHttp 拦截器自动注入 `Authorization` header，但 Retrofit 接口又用 `@Header("Authorization")` 手动传 token → 请求有两个同名 header。

虽然通常不会出错（Retrofit 的优先），但不规范。正确做法：拦截器统一注入，API 接口不传 token 参数。

## 架构决策

### 为什么用 WebView 而非原生终端？
- 复用后端已有的 xterm.js 前端
- 开发速度快，一套代码多端复用
- 缺点：性能不如原生，键盘适配有局限

### 为什么单 ViewModel？
- 项目规模不大（~2000 行 Kotlin）
- 状态之间有依赖（如文件操作需要 currentServer）
- 避免多个 ViewModel 之间的通信复杂度

### 为什么不用 Jetpack Navigation？
- 项目初期用 `sealed class Screen` + `when` 手动导航
- 简单直接，适合功能模块明确的项目
- 缺点：没有 deep link、没有转场动画

## 性能优化

1. **xterm.js 本地化**：避免每次打开终端都从 CDN 下载 280KB 的 JS
2. **Retrofit 连接池**：OkHttp 默认复用连接，减少握手开销
3. **Coroutine + ViewModelScope**：自动生命周期管理，避免内存泄漏
4. **DataStore 替代 SharedPreferences**：类型安全、协程原生支持

## 待改进

- [ ] SSH 终端改用原生渲染（如 Android Terminal Emulator 库）
- [ ] SFTP 断点续传
- [ ] 密钥文件导入（而非粘贴文本）
- [ ] 多语言支持
- [ ] 服务器连接状态实时检测
- [ ] 文件操作进度条

---
*总结于 2026-03-30，项目开发历时 1 天*
