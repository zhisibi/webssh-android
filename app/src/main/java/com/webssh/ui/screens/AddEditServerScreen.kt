package com.webssh.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webssh.api.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditServerScreen(
    existingServer: Server? = null,
    onSave: (name: String, host: String, port: Int, username: String, authType: String, password: String) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    onBack: () -> Unit
) {
    val isEditing = existingServer != null
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(existingServer?.name ?: "") }
    var host by remember { mutableStateOf(existingServer?.host ?: "") }
    var port by remember { mutableStateOf(existingServer?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(existingServer?.username ?: "root") }
    var authType by remember { mutableStateOf(existingServer?.authType ?: "password") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && host.isNotBlank() && port.isNotBlank() && username.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑服务器" else "添加服务器") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                placeholder = { Text("我的服务器") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // 主机
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("主机地址") },
                placeholder = { Text("192.168.1.100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // 端口
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // 用户名
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                placeholder = { Text("root") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // 认证方式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = authType == "password",
                    onClick = { authType = "password" },
                    label = { Text("密码认证") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = authType == "key",
                    onClick = { authType = "key" },
                    label = { Text("密钥认证") },
                    modifier = Modifier.weight(1f)
                )
            }

            // 密码
            if (authType == "password") {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "新密码（留空不修改）" else "密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    }
                )
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isEditing) "新密钥密码（留空不修改）" else "密钥密码（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏" else "显示"
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = {
                    onSave(name, host, port.toIntOrNull() ?: 22, username, authType, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = canSave,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) "保存修改" else "添加服务器", fontSize = 16.sp)
            }

            // 删除按钮（仅编辑模式）
            if (isEditing && onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除服务器", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除服务器「${existingServer?.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        existingServer?.let { onDelete?.invoke(it.id) }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
