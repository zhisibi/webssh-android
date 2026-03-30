package com.webssh.ui.screens

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webssh.viewmodel.UiState
import com.webssh.viewmodel.WebSSHViewModel
import com.webssh.viewmodel.WebSSHViewModelFactory

sealed class Screen {
    object Login : Screen()
    object ServerList : Screen()
    object AddEditServer : Screen()
    object FileManager : Screen()
    object SshTerminal : Screen()
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val viewModel: WebSSHViewModel = viewModel(factory = WebSSHViewModelFactory(context))

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var editingServer by remember { mutableStateOf<com.webssh.api.Server?>(null) }
    // Track where SSH terminal was opened from, for back navigation
    var sshBackTarget by remember { mutableStateOf<Screen>(Screen.ServerList) }

    val loginState by viewModel.loginState.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val token by viewModel.token.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortAsc by viewModel.sortAsc.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Check if already logged in
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            currentScreen = Screen.ServerList
        }
    }

    // File preview dialog
    if (fileContent != null && currentServer != null) {
        FilePreviewDialog(
            filename = "文件预览",
            content = fileContent,
            onDismiss = { viewModel.clearFileContent() }
        )
    }

    when (currentScreen) {
        Screen.Login -> {
            LoginScreen(
                baseUrl = baseUrl,
                onBaseUrlChange = { viewModel.setBaseUrl(it) },
                onLogin = { u, p -> viewModel.login(u, p) },
                loginState = loginState
            )
        }

        Screen.ServerList -> {
            ServerListScreen(
                servers = servers,
                onServerClick = { server ->
                    viewModel.selectServer(server)
                    currentScreen = Screen.FileManager
                },
                onSshClick = { server ->
                    viewModel.selectServer(server)
                    sshBackTarget = Screen.ServerList
                    currentScreen = Screen.SshTerminal
                },
                onAddServer = {
                    editingServer = null
                    currentScreen = Screen.AddEditServer
                },
                onEditServer = { server ->
                    editingServer = server
                    currentScreen = Screen.AddEditServer
                },
                onLogout = { viewModel.logout() },
                onSettingsClick = { /* TODO: Settings */ }
            )
        }

        Screen.AddEditServer -> {
            AddEditServerScreen(
                existingServer = editingServer,
                onSave = { name, host, port, username, authType, password ->
                    if (editingServer != null) {
                        viewModel.updateServer(editingServer!!.id, name, host, port, username, authType, password)
                    } else {
                        viewModel.addServer(name, host, port, username, authType, password)
                    }
                    currentScreen = Screen.ServerList
                },
                onDelete = { id ->
                    viewModel.deleteServer(id)
                    currentScreen = Screen.ServerList
                },
                onBack = { currentScreen = Screen.ServerList }
            )
        }

        Screen.FileManager -> {
            currentServer?.let { server ->
                FileManagerScreen(
                    serverName = server.name,
                    currentPath = currentPath,
                    files = files,
                    sortField = sortField,
                    sortAsc = sortAsc,
                    isLoading = isLoading,
                    onNavigateUp = { viewModel.navigateUp() },
                    onNavigate = { name ->
                        viewModel.navigateTo(if (currentPath == "/") "/$name" else "$currentPath/$name")
                    },
                    onRefresh = { viewModel.refresh() },
                    onSortChange = { viewModel.setSortField(it) },
                    onCreateFolder = { viewModel.createFolder(it) },
                    onDeleteFile = { name, type -> viewModel.deleteFile(name, type) },
                    onRenameFile = { oldName, newName -> viewModel.renameFile(oldName, newName) },
                    onDownloadFile = { name -> viewModel.downloadFile(name) },
                    onPreviewFile = { name -> viewModel.previewFile(name) },
                    onUploadFile = { name, content -> viewModel.uploadFile(name, content) },
                    onOpenSshTerminal = {
                        sshBackTarget = Screen.FileManager
                        currentScreen = Screen.SshTerminal
                    },
                    onBack = {
                        currentScreen = Screen.ServerList
                    }
                )
            }
        }

        Screen.SshTerminal -> {
            currentServer?.let { server ->
                SshTerminalScreen(
                    serverName = server.name,
                    serverId = server.id,
                    baseUrl = baseUrl,
                    token = token ?: "",
                    onBack = { currentScreen = sshBackTarget }
                )
            }
        }
    }
}
