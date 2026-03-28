package com.webssh.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webssh.viewmodel.UiState
import com.webssh.viewmodel.WebSSHViewModel
import com.webssh.viewmodel.WebSSHViewModelFactory

sealed class Screen {
    object Login : Screen()
    object ServerList : Screen()
    object FileManager : Screen()
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val viewModel: WebSSHViewModel = viewModel(factory = WebSSHViewModelFactory(context))
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    val loginState by viewModel.loginState.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortField by viewModel.sortField.collectAsState()
    val sortAsc by viewModel.sortAsc.collectAsState()

    // Check if already logged in
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            currentScreen = Screen.ServerList
        }
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
                onLogout = { viewModel.logout() },
                onSettingsClick = { /* TODO: Settings */ }
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
                    onNavigate = { name -> viewModel.navigateTo(if (currentPath == "/") "/$name" else "$currentPath/$name") },
                    onRefresh = { viewModel.refresh() },
                    onSortChange = { viewModel.setSortField(it) },
                    onCreateFolder = { viewModel.createFolder(it) },
                    onDeleteFile = { name, type -> viewModel.deleteFile(name, type) },
                    onRenameFile = { oldName, newName -> viewModel.renameFile(oldName, newName) },
                    onBack = {
                        viewModel.selectServer(server.copy())
                        currentScreen = Screen.ServerList
                    }
                )
            }
        }
    }
}