package com.webssh.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    serverName: String,
    serverId: Long,
    baseUrl: String,
    token: String,
    onBack: () -> Unit
) {
    var connectionStatus by remember { mutableStateOf("连接中...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(serverName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            connectionStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                errorMessage != null -> MaterialTheme.colorScheme.error
                                connectionStatus == "已连接" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (token.isEmpty()) {
                // No token - show error
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未登录，无法连接 SSH", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("返回") }
                }
            } else {
                // Build WebSocket URL
                val wsBase = baseUrl
                    .replace("http://", "ws://")
                    .replace("https://", "wss://")
                    .trimEnd('/')
                val wsUrl = "$wsBase/ws/ssh?serverId=$serverId&token=$token"
                val terminalHtml = remember(wsUrl) { buildTerminalHtml(wsUrl) }

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(false)

                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onConnected() {
                                    connectionStatus = "已连接"
                                    errorMessage = null
                                }

                                @JavascriptInterface
                                fun onDisconnected(reason: String) {
                                    connectionStatus = "已断开"
                                    errorMessage = reason
                                }

                                @JavascriptInterface
                                fun onError(error: String) {
                                    connectionStatus = "连接失败"
                                    errorMessage = error
                                }
                            }, "AndroidBridge")

                            loadDataWithBaseURL(
                                "https://cdn.jsdelivr.net/",
                                terminalHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Error overlay
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SSH 连接失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage ?: "未知错误", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onBack) { Text("返回") }
                        }
                    }
                }

                // Loading indicator
                if (connectionStatus == "连接中..." && errorMessage == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

private fun buildTerminalHtml(wsUrl: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>SSH Terminal</title>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.min.css">
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: 100%; height: 100%; overflow: hidden; background: #1e1e1e; }
#terminal { width: 100%; height: 100%; }
.xterm { padding: 4px; }
</style>
</head>
<body>
<div id="terminal"></div>
<script src="https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.min.js"></script>
<script>
(function() {
    try {
        var term = new Terminal({
            cursorBlink: true,
            fontSize: 14,
            fontFamily: 'Menlo, Monaco, "Courier New", monospace',
            theme: {
                background: '#1e1e1e',
                foreground: '#f0f0f0',
                cursor: '#ffffff',
                cursorAccent: '#1e1e1e',
                selectionBackground: '#264f78'
            }
        });

        var fitAddon = new FitAddon.FitAddon();
        term.loadAddon(fitAddon);
        term.open(document.getElementById('terminal'));

        function doFit() {
            try { fitAddon.fit(); } catch(e) {}
        }
        doFit();
        window.addEventListener('resize', doFit);

        var ws = null;

        function connect() {
            try {
                ws = new WebSocket("$wsUrl");
            } catch(e) {
                term.write('\\r\\n\\x1b[31m[WebSocket 创建失败: ' + e.message + ']\\x1b[0m\\r\\n');
                try { AndroidBridge.onError('WebSocket 创建失败: ' + e.message); } catch(x) {}
                return;
            }

            ws.onopen = function() {
                try { AndroidBridge.onConnected(); } catch(e) {}
                try {
                    ws.send(JSON.stringify({ type: 'resize', cols: term.cols, rows: term.rows }));
                } catch(e) {}
            };

            ws.onmessage = function(event) {
                try {
                    var msg = JSON.parse(event.data);
                    if (msg.type === 'data') {
                        term.write(msg.data);
                    } else if (msg.type === 'connected') {
                        // session ready
                    } else if (msg.type === 'close') {
                        term.write('\\r\\n\\x1b[31m[连接已关闭: ' + (msg.data || '') + ']\\x1b[0m\\r\\n');
                        try { AndroidBridge.onDisconnected(msg.data || 'closed'); } catch(e) {}
                    } else if (msg.type === 'error') {
                        term.write('\\r\\n\\x1b[31m[错误: ' + msg.data + ']\\x1b[0m\\r\\n');
                        try { AndroidBridge.onError(msg.data); } catch(e) {}
                    }
                } catch(e) {
                    // Non-JSON message, write directly
                    term.write(event.data);
                }
            };

            ws.onclose = function(event) {
                var reason = event.code + (event.reason ? ': ' + event.reason : '');
                term.write('\\r\\n\\x1b[31m[连接断开: ' + reason + ']\\x1b[0m\\r\\n');
                try { AndroidBridge.onDisconnected(reason); } catch(e) {}
            };

            ws.onerror = function(err) {
                term.write('\\r\\n\\x1b[31m[WebSocket 连接错误 - 请检查服务器地址和网络]\\x1b[0m\\r\\n');
                try { AndroidBridge.onError('WebSocket 连接错误'); } catch(e) {}
            };
        }

        term.onData(function(data) {
            if (ws && ws.readyState === WebSocket.OPEN) {
                try {
                    ws.send(JSON.stringify({ type: 'data', data: data }));
                } catch(e) {}
            }
        });

        // Delay connection slightly to let WebView fully initialize
        setTimeout(connect, 500);

    } catch(e) {
        document.body.innerHTML = '<div style="color:red;padding:20px;">终端初始化失败: ' + e.message + '</div>';
        try { AndroidBridge.onError('终端初始化失败: ' + e.message); } catch(x) {}
    }
})();
</script>
</body>
</html>
""".trimIndent()
}
