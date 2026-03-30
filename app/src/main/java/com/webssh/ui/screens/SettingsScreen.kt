package com.webssh.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.webssh.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: UiState<String>,
    backupContent: String?,
    onChangePassword: (String, String) -> Unit,
    onBackup: () -> Unit,
    onRestore: (String) -> Unit,
    onClearState: () -> Unit,
    onClearBackup: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBackupResult by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }

    LaunchedEffect(settingsState) {
        when (settingsState) {
            is UiState.Success -> {
                Toast.makeText(context, settingsState.data, Toast.LENGTH_SHORT).show()
                if (backupContent != null) showBackupResult = true
                onClearState()
            }
            is UiState.Error -> {
                Toast.makeText(context, settingsState.message, Toast.LENGTH_SHORT).show()
                onClearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Data section
            Text("数据管理", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text("数据备份") },
                        supportingContent = { Text("导出服务器列表配置") },
                        leadingContent = { Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable { onBackup() }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("数据恢复") },
                        supportingContent = { Text("从备份 JSON 恢复") },
                        leadingContent = { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable {
                            restoreText = ""
                            showRestoreDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account section
            Text("账户", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                ListItem(
                    headlineContent = { Text("修改密码") },
                    supportingContent = { Text("修改管理员登录密码") },
                    leadingContent = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { showPasswordDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // About
            Text("关于", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                ListItem(
                    headlineContent = { Text("WebSSH Android") },
                    supportingContent = { Text("版本 1.1.0 · Phase 2") },
                    leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }

    // Password dialog
    if (showPasswordDialog) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var showPw by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = oldPw, onValueChange = { oldPw = it },
                        label = { Text("当前密码") }, singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { showPw = !showPw }) { Icon(if (showPw) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPw, onValueChange = { newPw = it },
                        label = { Text("新密码") }, singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPw, onValueChange = { confirmPw = it },
                        label = { Text("确认新密码") }, singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPw.isNotEmpty() && newPw != confirmPw,
                        supportingText = if (confirmPw.isNotEmpty() && newPw != confirmPw) ({ Text("两次输入不一致", color = MaterialTheme.colorScheme.error) }) else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onChangePassword(oldPw, newPw); showPasswordDialog = false }, enabled = oldPw.isNotBlank() && newPw.isNotBlank() && newPw == confirmPw) { Text("修改") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("取消") } }
        )
    }

    // Restore dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("数据恢复") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("粘贴备份的 JSON 内容：")
                    OutlinedTextField(
                        value = restoreText, onValueChange = { restoreText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("粘贴 JSON 数据...") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onRestore(restoreText); showRestoreDialog = false }, enabled = restoreText.isNotBlank()) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("取消") } }
        )
    }

    // Backup result dialog
    if (showBackupResult && backupContent != null) {
        AlertDialog(
            onDismissRequest = { showBackupResult = false; onClearBackup() },
            title = { Text("备份数据") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请妥善保存：")
                    OutlinedTextField(
                        value = backupContent, onValueChange = {},
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        readOnly = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, backupContent); type = "text/plain" }
                        context.startActivity(Intent.createChooser(sendIntent, "分享备份"))
                    }) { Text("分享") }
                    TextButton(onClick = {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("backup", backupContent))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }) { Text("复制") }
                }
            },
            dismissButton = { TextButton(onClick = { showBackupResult = false; onClearBackup() }) { Text("关闭") } }
        )
    }
}
