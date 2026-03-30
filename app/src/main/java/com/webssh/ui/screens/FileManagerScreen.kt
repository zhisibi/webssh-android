@file:OptIn(ExperimentalMaterial3Api::class)

package com.webssh.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webssh.api.FileItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileManagerScreen(
    serverName: String,
    currentPath: String,
    files: List<FileItem>,
    sortField: String,
    sortAsc: Boolean,
    isLoading: Boolean,
    onNavigateUp: () -> Unit,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onSortChange: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (String, String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onDownloadFile: (String) -> Unit,
    onPreviewFile: (String) -> Unit,
    onUploadFile: (String, String) -> Unit,
    onOpenSshTerminal: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }

    // File picker for upload
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "uploaded_file"
            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
            if (bytes != null) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                onUploadFile(fileName, base64)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(serverName, style = MaterialTheme.typography.titleMedium)
                        Text(currentPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSshTerminal) {
                        Icon(Icons.Default.Computer, contentDescription = "SSH终端")
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "上传文件")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sort chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip("名称", "name", sortField, sortAsc, onSortChange)
                SortChip("大小", "size", sortField, sortAsc, onSortChange)
                SortChip("时间", "mtime", sortField, sortAsc, onSortChange)
            }

            // Navigate up button
            if (currentPath != "/") {
                ListItem(
                    headlineContent = { Text("..") },
                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateUp() }
                )
                Divider()
            }

            // File list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("空文件夹", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn {
                    items(files) { file ->
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.type == "directory") {
                                    onNavigate(file.name)
                                } else {
                                    // Click file to download
                                    onDownloadFile(file.name)
                                }
                            },
                            onPreview = {
                                selectedFile = file
                                onPreviewFile(file.name)
                            },
                            onDownload = {
                                onDownloadFile(file.name)
                            },
                            onRename = {
                                selectedFile = file
                                newFileName = file.name
                                showRenameDialog = true
                            },
                            onDelete = {
                                selectedFile = file
                                showDeleteDialog = true
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Create folder dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotEmpty()) {
                            onCreateFolder(newFolderName)
                            newFolderName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${selectedFile!!.name} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(selectedFile!!.name, selectedFile!!.type)
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Rename dialog
    if (showRenameDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotEmpty() && newFileName != selectedFile!!.name) {
                            onRenameFile(selectedFile!!.name, newFileName)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = formatFileSize(file.size) + " • " + formatTime(file.mtime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = when (file.type) {
                    "directory" -> Icons.Default.Folder
                    "link" -> Icons.Default.Link
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when (file.type) {
                    "directory" -> MaterialTheme.colorScheme.primary
                    "link" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (file.type == "file") {
                        DropdownMenuItem(
                            text = { Text("预览") },
                            onClick = {
                                showMenu = false
                                onPreview()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("下载") },
                            onClick = {
                                showMenu = false
                                onDownload()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun FilePreviewDialog(
    filename: String,
    content: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(filename) },
        text = {
            if (content != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun SortChip(
    label: String,
    field: String,
    currentField: String,
    ascending: Boolean,
    onClick: (String) -> Unit
) {
    val isSelected = currentField == field
    FilterChip(
        selected = isSelected,
        onClick = { onClick(field) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (isSelected) {
                    Icon(
                        if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}
