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
    var isConnected by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("连接中...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(serverName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            connectionStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
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
            // Build WebSocket URL from baseUrl
            val wsBase = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
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

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onConnected() {
                                isConnected = true
                                connectionStatus = "已连接"
                            }

                            @JavascriptInterface
                            fun onDisconnected(reason: String) {
                                isConnected = false
                                connectionStatus = "已断开: $reason"
                            }

                            @JavascriptInterface
                            fun onError(error: String) {
                                connectionStatus = "错误: $error"
                            }
                        }, "AndroidBridge")

                        loadDataWithBaseURL(
                            "file:///android_asset/",
                            terminalHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isConnected && connectionStatus.startsWith("连接中")) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
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
    var term = new Terminal({
        cursorBlink: true,
        fontSize: 14,
        fontFamily: 'Menlo, Monaco, "Courier New", monospace',
        theme: {
            background: '#1e1e1e',
            foreground: '#f0f0f0',
            cursor: '#ffffff',
            cursorAccent: '#1e1e1e',
            selectionBackground: '#264f78',
            black: '#000000',
            red: '#cd3131',
            green: '#0dbc79',
            yellow: '#e5e510',
            blue: '#2472c8',
            magenta: '#bc3fbc',
            cyan: '#11a8cd',
            white: '#e5e5e5',
            brightBlack: '#666666',
            brightRed: '#f14c4c',
            brightGreen: '#23d18b',
            brightYellow: '#f5f543',
            brightBlue: '#3b8eea',
            brightMagenta: '#d670d6',
            brightCyan: '#29b8db',
            brightWhite: '#e5e5e5'
        }
    });

    var fitAddon = new FitAddon.FitAddon();
    term.loadAddon(fitAddon);
    term.open(document.getElementById('terminal'));

    function fit() {
        try { fitAddon.fit(); } catch(e) {}
    }
    fit();
    window.addEventListener('resize', fit);

    var ws = null;
    var connected = false;

    function connect() {
        ws = new WebSocket("$wsUrl");

        ws.onopen = function() {
            connected = true;
            try { AndroidBridge.onConnected(); } catch(e) {}
            var dims = { cols: term.cols, rows: term.rows };
            ws.send(JSON.stringify({ type: 'resize', cols: dims.cols, rows: dims.rows }));
        };

        ws.onmessage = function(event) {
            try {
                var msg = JSON.parse(event.data);
                if (msg.type === 'data') {
                    term.write(msg.data);
                } else if (msg.type === 'connected') {
                    // session ready
                } else if (msg.type === 'close') {
                    connected = false;
                    term.write('\r\n\x1b[31m[连接已关闭]\x1b[0m\r\n');
                    try { AndroidBridge.onDisconnected(msg.data || 'closed'); } catch(e) {}
                } else if (msg.type === 'error') {
                    term.write('\r\n\x1b[31m[错误: ' + msg.data + ']\x1b[0m\r\n');
                    try { AndroidBridge.onError(msg.data); } catch(e) {}
                }
            } catch(e) {
                term.write(event.data);
            }
        };

        ws.onclose = function() {
            connected = false;
            term.write('\r\n\x1b[31m[连接断开]\x1b[0m\r\n');
            try { AndroidBridge.onDisconnected('Connection closed'); } catch(e) {}
        };

        ws.onerror = function(err) {
            term.write('\r\n\x1b[31m[WebSocket 错误]\x1b[0m\r\n');
            try { AndroidBridge.onError('WebSocket error'); } catch(e) {}
        };
    }

    term.onData(function(data) {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: 'data', data: data }));
        }
    });

    connect();
})();
</script>
</body>
</html>
""".trimIndent()
}
