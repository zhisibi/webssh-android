package com.webssh.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webssh.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class WebSSHViewModel(
    private val context: Context
) : ViewModel() {

    private val tokenManager = TokenManager(context)
    private val networkClient = NetworkClient(tokenManager)

    private val _baseUrl = MutableStateFlow("http://192.168.100.20:3000")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    private val _currentServer = MutableStateFlow<Server?>(null)
    val currentServer: StateFlow<Server?> = _currentServer.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortField = MutableStateFlow("name")
    val sortField: StateFlow<String> = _sortField.asStateFlow()

    private val _sortAsc = MutableStateFlow(true)
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    private var api: WebSSHApi? = null

    init {
        viewModelScope.launch {
            tokenManager.baseUrl.collect { url ->
                _baseUrl.value = url
                api = networkClient.createApi(url)
            }
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            tokenManager.saveBaseUrl(url)
            api = networkClient.createApi(url)
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                val response = api?.login(LoginRequest(username, password))
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    val token = response.body()?.token
                    if (token != null) {
                        tokenManager.saveToken(token)
                        _loginState.value = UiState.Success(Unit)
                        loadServers()
                    } else {
                        _loginState.value = UiState.Error(response.body()?.message ?: "登录失败")
                    }
                } else {
                    _loginState.value = UiState.Error(response?.body()?.message ?: "登录失败")
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                tokenManager.token.first()?.let { token ->
                    api?.logout("Bearer $token")
                }
            } catch (_: Exception) { }
            tokenManager.clearToken()
            _loginState.value = UiState.Idle
            _servers.value = emptyList()
            _currentServer.value = null
            _files.value = emptyList()
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api?.getServers()
                if (response?.isSuccessful == true) {
                    _servers.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
            _isLoading.value = false
        }
    }

    fun selectServer(server: Server) {
        _currentServer.value = server
        _currentPath.value = "/"
        loadFiles()
    }

    fun navigateTo(path: String) {
        _currentPath.value = path
        loadFiles()
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current != "/") {
            val parent = current.substringBeforeLast('/', current)
            _currentPath.value = if (parent.isEmpty()) "/" else parent
            loadFiles()
        }
    }

    fun loadFiles() {
        val server = _currentServer.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api?.listFiles(server.id, _currentPath.value)
                if (response?.isSuccessful == true) {
                    val fileList = response.body()?.files ?: emptyList()
                    _files.value = sortFiles(fileList)
                }
            } catch (e: Exception) {
                // Handle error
            }
            _isLoading.value = false
        }
    }

    fun setSortField(field: String) {
        if (_sortField.value == field) {
            _sortAsc.value = !_sortAsc.value
        } else {
            _sortField.value = field
            _sortAsc.value = true
        }
        _files.value = sortFiles(_files.value)
    }

    private fun sortFiles(fileList: List<FileItem>): List<FileItem> {
        val dirs = fileList.filter { it.type == "directory" }
        val others = fileList.filter { it.type != "directory" }
        val sortedDirs = if (_sortAsc.value) {
            dirs.sortedBy { it.name.lowercase() }
        } else {
            dirs.sortedByDescending { it.name.lowercase() }
        }
        val sortedOthers = when (_sortField.value) {
            "name" -> if (_sortAsc.value) others.sortedBy { it.name.lowercase() } else others.sortedByDescending { it.name.lowercase() }
            "size" -> if (_sortAsc.value) others.sortedBy { it.size } else others.sortedByDescending { it.size }
            "mtime" -> if (_sortAsc.value) others.sortedBy { it.mtime } else others.sortedByDescending { it.mtime }
            else -> others
        }
        return sortedDirs + sortedOthers
    }

    fun createFolder(name: String) {
        val server = _currentServer.value ?: return
        viewModelScope.launch {
            try {
                val response = api?.createFolder(
                    MkdirRequest(server.id, _currentPath.value, name)
                )
                if (response?.isSuccessful == true) {
                    loadFiles()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteFile(name: String, type: String) {
        val server = _currentServer.value ?: return
        val fullPath = if (_currentPath.value == "/") "/$name" else "${_currentPath.value}/$name"
        viewModelScope.launch {
            try {
                val response = api?.deleteFile(
                    DeleteRequest(server.id, fullPath, type)
                )
                if (response?.isSuccessful == true) {
                    loadFiles()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun renameFile(oldName: String, newName: String) {
        val server = _currentServer.value ?: return
        val oldPath = if (_currentPath.value == "/") "/$oldName" else "${_currentPath.value}/$oldName"
        val newPath = if (_currentPath.value == "/") "/$newName" else "${_currentPath.value}/$newName"
        viewModelScope.launch {
            try {
                val response = api?.renameFile(
                    RenameRequest(server.id, oldPath, newPath)
                )
                if (response?.isSuccessful == true) {
                    loadFiles()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun refresh() {
        loadFiles()
    }
}

class WebSSHViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebSSHViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebSSHViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
