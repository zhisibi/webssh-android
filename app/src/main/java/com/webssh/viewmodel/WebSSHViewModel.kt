package com.webssh.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent: StateFlow<String?> = _fileContent.asStateFlow()

    private var api: WebSSHApi? = null

    init {
        viewModelScope.launch {
            tokenManager.baseUrl.collect { url ->
                _baseUrl.value = url
                api = networkClient.createApi(url)
            }
        }
        viewModelScope.launch {
            tokenManager.token.collect { t ->
                _token.value = t
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

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
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.getServers("Bearer $token")
                if (response?.isSuccessful == true) {
                    _servers.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
            _isLoading.value = false
        }
    }

    // ==================== Server CRUD ====================

    fun addServer(name: String, host: String, port: Int, username: String, authType: String, password: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.addServer(
                    "Bearer $token",
                    ServerRequest(name, host, port, username, authType, password, emptyList(), true)
                )
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _toastMessage.value = "服务器添加成功"
                    loadServers()
                } else {
                    _toastMessage.value = response?.body()?.message ?: "添加失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    fun updateServer(id: Long, name: String, host: String, port: Int, username: String, authType: String, password: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.updateServer(
                    "Bearer $token",
                    id,
                    ServerRequest(name, host, port, username, authType, password, emptyList(), true)
                )
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _toastMessage.value = "服务器更新成功"
                    loadServers()
                } else {
                    _toastMessage.value = response?.body()?.message ?: "更新失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.deleteServer("Bearer $token", id)
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _toastMessage.value = "服务器已删除"
                    loadServers()
                } else {
                    _toastMessage.value = response?.body()?.message ?: "删除失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    // ==================== Server Selection & Navigation ====================

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
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.listFiles("Bearer $token", server.id, _currentPath.value)
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
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.createFolder(
                    "Bearer $token",
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
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.deleteFile(
                    "Bearer $token",
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
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.renameFile(
                    "Bearer $token",
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

    // ==================== File Upload ====================

    fun uploadFile(filename: String, base64Content: String) {
        val server = _currentServer.value ?: return
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.uploadFile(
                    "Bearer $token",
                    UploadRequest(server.id, _currentPath.value, filename, base64Content)
                )
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _toastMessage.value = "上传成功"
                    loadFiles()
                } else {
                    _toastMessage.value = response?.body()?.message ?: "上传失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    // ==================== File Download ====================

    fun downloadFile(name: String) {
        val server = _currentServer.value ?: return
        val fullPath = if (_currentPath.value == "/") "/$name" else "${_currentPath.value}/$name"
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.downloadFile("Bearer $token", server.id, fullPath)
                if (response?.isSuccessful == true) {
                    val body = response.body()
                    if (body != null) {
                        val bytes = body.bytes()
                        saveFileToDownloads(name, bytes)
                    }
                } else {
                    _toastMessage.value = "下载失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    private fun saveFileToDownloads(filename: String, bytes: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    _toastMessage.value = "已保存到下载目录: $filename"
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                file.writeBytes(bytes)
                _toastMessage.value = "已保存到: ${file.absolutePath}"
            }
        } catch (e: Exception) {
            _toastMessage.value = "保存文件失败: ${e.message}"
        }
    }

    // ==================== File Preview ====================

    fun previewFile(name: String) {
        val server = _currentServer.value ?: return
        val fullPath = if (_currentPath.value == "/") "/$name" else "${_currentPath.value}/$name"
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first() ?: return@launch
                val response = api?.readFile("Bearer $token", server.id, fullPath)
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _fileContent.value = response.body()?.content
                } else {
                    _toastMessage.value = "预览失败"
                }
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "网络错误"
            }
        }
    }

    fun clearFileContent() { _fileContent.value = null }

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
