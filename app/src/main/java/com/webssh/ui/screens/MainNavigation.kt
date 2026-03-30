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
    object Settings : Screen()
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val viewModel: WebSSHViewModel = viewModel(factory = WebSSHViewModelFactory(context))

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var editingServer by remember { mutableStateOf<com.webssh.api.Server?>(null) }
    var sshBackTarget by remember { mutableStateOf<Screen>(Screen.ServerList) }

    val loginState by viewModel.loginState.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val token by viewModel.token.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val filteredServers by viewModel.filteredServers.collectAsState()
    val allTags = remember(servers) { viewModel.getAllTags() }
    val tagFilter by viewModel.tagFilter.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortAsc by viewModel.sortAsc.collectAsState()
    val batchMode by viewModel.batchMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val backupContent by viewModel.backupContent.collectAsState()

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // React to login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is UiState.Success -> {
                currentScreen = Screen.ServerList
            }
            is UiState.Idle -> {
                currentScreen = Screen.Login
            }
            else -> {}
        }
    }

    // File preview dialog
    if (fileContent != null && currentServer != null) {
        FilePreviewDialog(filename = "文件预览", content = fileContent, onDismiss = { viewModel.clearFileContent() })
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
                filteredServers = filteredServers,
                allTags = allTags,
                currentTagFilter = tagFilter,
                onServerClick = { server ->
                    viewModel.selectServer(server)
                    currentScreen = Screen.FileManager
                },
                onSshClick = { server ->
                    viewModel.selectServer(server)
                    sshBackTarget = Screen.ServerList
                    currentScreen = Screen.SshTerminal
                },
                onTagFilter = { tag -> viewModel.setTagFilter(tag) },
                onAddServer = { editingServer = null; currentScreen = Screen.AddEditServer },
                onEditServer = { server -> editingServer = server; currentScreen = Screen.AddEditServer },
                onLogout = { viewModel.logout() },
                onSettingsClick = { currentScreen = Screen.Settings }
            )
        }

        Screen.AddEditServer -> {
            AddEditServerScreen(
                existingServer = editingServer,
                onSave = { name, host, port, username, authType, password, tags, privateKey, passphrase ->
                    if (editingServer != null) {
                        viewModel.updateServer(editingServer!!.id, name, host, port, username, authType, password, tags, editingServer!!.enabled, privateKey, passphrase)
                    } else {
                        viewModel.addServer(name, host, port, username, authType, password, tags, privateKey, passphrase)
                    }
                    currentScreen = Screen.ServerList
                },
                onDelete = { id -> viewModel.deleteServer(id); currentScreen = Screen.ServerList },
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
                    batchMode = batchMode,
                    selectedFiles = selectedFiles,
                    onNavigateUp = { viewModel.navigateUp() },
                    onNavigate = { name -> viewModel.navigateTo(if (currentPath == "/") "/$name" else "$currentPath/$name") },
                    onRefresh = { viewModel.refresh() },
                    onSortChange = { viewModel.setSortField(it) },
                    onCreateFolder = { viewModel.createFolder(it) },
                    onDeleteFile = { name, type -> viewModel.deleteFile(name, type) },
                    onRenameFile = { old, new -> viewModel.renameFile(old, new) },
                    onDownloadFile = { name -> viewModel.downloadFile(name) },
                    onPreviewFile = { name -> viewModel.previewFile(name) },
                    onUploadFile = { name, content -> viewModel.uploadFile(name, content) },
                    onOpenSshTerminal = { sshBackTarget = Screen.FileManager; currentScreen = Screen.SshTerminal },
                    onToggleBatchMode = { viewModel.toggleBatchMode() },
                    onToggleFileSelection = { name -> viewModel.toggleFileSelection(name) },
                    onSelectAll = { viewModel.selectAllFiles() },
                    onBatchDownload = { viewModel.batchDownloadZip() },
                    onClearSelection = { viewModel.clearSelection() },
                    onBack = { currentScreen = Screen.ServerList }
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

        Screen.Settings -> {
            SettingsScreen(
                settingsState = settingsState,
                backupContent = backupContent,
                onChangePassword = { old, new -> viewModel.changePassword(old, new) },
                onBackup = { viewModel.backupServers() },
                onRestore = { content -> viewModel.restoreServers(content) },
                onClearState = { viewModel.clearSettingsState() },
                onClearBackup = { viewModel.clearBackupContent() },
                onBack = { currentScreen = Screen.ServerList }
            )
        }
    }
}
