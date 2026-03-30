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
                // Build WebSocket URL — use ws:// or wss:// based on baseUrl scheme
                val wsBase = baseUrl
                    .replace("http://", "ws://")
                    .replace("https://", "wss://")
                    .trimEnd('/')
                val wsUrl = "$wsBase/ws/ssh?serverId=$serverId&token=$token"

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
                            settings.mediaPlaybackRequiresUserGesture = false

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    connectionStatus = "已连接"
                                }
                            }

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

                            // Load from local assets (file:// protocol allows ws:// WebSocket)
                            // Inject the WebSocket URL by replacing placeholder
                            val htmlTemplate = context.assets.open("ssh_terminal.html")
                                .bufferedReader()
                                .readText()
                            val html = htmlTemplate.replace("%WS_URL%", wsUrl)

                            loadDataWithBaseURL(
                                "file:///android_asset/",
                                html,
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
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
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
