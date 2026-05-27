package com.deividsrk.droidcoder.ui.explorer

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deividsrk.droidcoder.ui.MainViewModel

@Composable
fun FileExplorerScreen(viewModel: MainViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val fileContent by viewModel.fileContent.collectAsStateWithLifecycle()
    val isPushing by viewModel.isPushing.collectAsStateWithLifecycle()
    val gitLog by viewModel.gitLog.collectAsStateWithLifecycle()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var commitMessage by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // File list sidebar
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Repositório",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    IconButton(
                        onClick = { showNewFileDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Novo arquivo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // File list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(4.dp)
            ) {
                if (files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nenhum arquivo.\nSelecione uma pasta ou clone um repositório.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(files) { file ->
                    Surface(
                        onClick = { viewModel.selectFile(file) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (selectedFile == file)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedFile == file)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = file,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (selectedFile == file)
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Normal,
                                color = if (selectedFile == file)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showDeleteConfirm = file },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Excluir",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Git push panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.GitBranch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Enviar ao GitHub",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mensagem do commit...", style = MaterialTheme.typography.bodySmall) },
                        textStyle = TextStyle(fontSize = 13.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (commitMessage.isNotBlank()) {
                                viewModel.commitAndPush(commitMessage.trim())
                                commitMessage = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPushing && commitMessage.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isPushing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enviando...")
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Commit & Push")
                        }
                    }
                }
            }
        }

        // Code editor
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedFile != null) {
                // Editor header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                selectedFile!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.saveFile() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Salvar")
                        }
                    }
                }

                // Editor text field
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = fileContent,
                        onValueChange = { /* handled by ViewModel via state hoisting */ },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                if (fileContent.isEmpty()) {
                                    Text(
                                        "// Arquivo vazio",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Selecione um arquivo para editar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Git log terminal
            AnimatedVisibility(
                visible = gitLog != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                gitLog?.let { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Console Git",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = log,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Novo Arquivo") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Nome do arquivo") },
                    placeholder = { Text("ex: src/main.kt") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        viewModel.createFile(newFileName.trim())
                        newFileName = ""
                        showNewFileDialog = false
                    }
                }) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation
    showDeleteConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Excluir arquivo?") },
            text = { Text("Tem certeza que deseja excluir \"$file\" permanentemente?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
