# WebSSH Android 开发经验总结

## 项目历程

从零开始，3 个阶段迭代 + 多轮调试，1 天内完成全功能 Android SSH 客户端。

## 关键经验

### 1. WebView + WebSocket + xterm.js 的坑

**混合内容安全策略**：Android WebView 加载 HTTPS 页面时，浏览器禁止 `ws://` 连接。
- ❌ 从 CDN 加载 xterm.js（HTTPS）→ ws:// 被拦截
- ✅ xterm.js 下载到 `assets/`，通过 `file://` 协议加载

**WebSocket URL 参数对齐**：后端读 `?server=` 但前端发 `?serverId=`，一个字母的差异导致连接失败。
- 教训：前后端联调必须严格核对参数名，写好 API 文档。

### 2. Android WebView 键盘适配

这是整个项目最棘手的问题，前后改了 6 个版本。

**尝试过的方案**：
| 方案 | 问题 |
|------|------|
| `adjustResize` + visualViewport 动态高度 | 不稳定，终端内容跳动 |
| `65vh` 固定高度 | vh 用原始视口，键盘弹出后仍被遮挡 |
| `visualViewport.height - toolbar` 动态 | ResizeObserver 时序问题 |

**最终方案**：固定比例布局
- 终端：`height: 45vh`（屏幕高度的 45%，固定）
- 工具栏：`position: fixed; top: 0`（固定顶部）
- 终端 `margin-top: 40px`（工具栏下方）

**教训**：移动端 Web 终端的键盘适配是业界难题，固定比例比动态调整更可靠。

### 3. 后端 WebSocket 错误处理

Node.js EventEmitter 没有 error listener 时会直接抛异常崩溃进程。

```javascript
// 防崩溃三件套
ws.on('error', (err) => { /* 兜底 */ });
function safeSend(data) {
  try { if (ws.readyState === WebSocket.OPEN) ws.send(data); } catch(e) {}
}
conn.on('error', (err) => {
  safeSend(JSON.stringify({ type: 'error', data: err.message }));
  try { ws.close(); } catch(e) {}
});
```

### 4. Adaptive Icon 兼容性

Android 8+ 使用 Adaptive Icon，普通 `mipmap-*/ic_launcher.png` 会被系统默认图标覆盖。
- 需要 `mipmap-anydpi-v26/ic_launcher.xml` 定义前景 + 背景
- 前景层：自定义图标
- 背景层：纯色 `#1e1e1e`

### 5. 指纹登录实现

基于已有的"记住密码"功能，加一层 BiometricPrompt 验证：
- 设置页：开关控制启用/禁用（需先通过指纹验证才能开启）
- 登录页：启用后显示"指纹登录"按钮
- `BiometricPrompt` 需要 `FragmentActivity`（不是 `ComponentActivity`）
- 不需要 `CryptoObject`，成功后直接用已保存的凭据登录

### 6. 前后端密码存储

后端对密码做 AES-256-GCM 加密。更新操作如果发空密码会覆盖旧值。
- 教训：更新操作应保留旧密码，只在用户明确修改时覆盖。

### 7. Compose 图标可用性

`material-icons-extended` 包含更多图标，但 `Icons.Default.*` 不一定都有。
- `Icons.Default.Fingerprint` → 不可用
- `Icons.Default.Lock` → 不可用
- `Icons.Default.Settings` → 可用
- 解决：用 emoji 文本替代（🔐）

## 架构决策

| 决策 | 原因 |
|------|------|
| WebView 而非原生终端 | 复用后端 xterm.js，开发快 |
| 单 ViewModel | 项目规模小，状态间有依赖 |
| sealed class 导航 | 简单直接，无 deep link 需求 |
| DataStore 替代 SharedPreferences | 类型安全、协程原生支持 |
| 固定比例终端布局 | 比动态调整更稳定 |

## 待改进

- [ ] SSH 终端改用原生渲染（如 Android Terminal Emulator 库）
- [ ] SFTP 断点续传
- [ ] 密钥文件导入（而非粘贴文本）
- [ ] 多语言支持
- [ ] 文件操作进度条
- [ ] 服务器连接状态实时检测

## 开发时间线

| 时间 | 里程碑 |
|------|--------|
| 09:00 | 读取 API 文档，制定三阶段计划 |
| 09:11 | Phase 1 开始：服务器 CRUD + SSH + SFTP |
| 09:38 | Phase 1 完成，首次构建 APK |
| 10:00 | Phase 2：批量下载 + 设置 + 标签 |
| 10:29 | Phase 2 完成 |
| 10:47 | 修复 4 个 Bug（禁用崩溃、SSH、退出、文件浏览） |
| 11:51 | Phase 3：密钥认证 + 搜索 + 暗色主题 |
| 13:29 | SSH 混合内容修复 |
| 14:16 | 后端 WebSocket 防崩溃 |
| 14:48 | SSH 参数名修复（serverId → server） |
| 16:17 | SSH 终端键盘适配 + 快捷键工具栏 |
| 18:10 | 自定义应用图标 |
| 19:00 | 指纹生物识别登录 |
| 20:21 | 项目整理，完善文档 |

---
*总结于 2026-03-30*
