package com.deividsrk.droidcoder.ui.explorer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deividsrk.droidcoder.ui.MainViewModel
import com.deividsrk.droidcoder.ui.util.DraculaSyntaxTransformation

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
    var showHtmlPreview by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure AMOLED Black
    ) {
        // File list sidebar
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .border(width = (0.5).dp, color = Color(0xFF282A36)) // Dracula thin border separator
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0C0D12), // Dracula current line gray
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF8BE9FD), // Dracula Cyan
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "REPOSITÓRIO",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF8BE9FD),
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(
                        onClick = { showNewFileDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Novo arquivo",
                            tint = Color(0xFFA6E22E), // Dracula Green
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // File list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF000000)),
                contentPadding = PaddingValues(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                color = Color(0xFF6272A4), // Dracula Comment Gray
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(files) { file ->
                    val isSelected = selectedFile == file
                    Surface(
                        onClick = { viewModel.selectFile(file) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) Color(0xFF1D1F2B) else Color.Transparent, // Dracula Selection
                        shape = RoundedCornerShape(4.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke((0.5).dp, Color(0xFFFF79C6)) else null // Dracula Pink border for selection
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
                                tint = if (isSelected) Color(0xFFFF79C6) else Color(0xFF8BE9FD) // Pink if active, else Cyan
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = file,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                                color = if (isSelected) Color(0xFFFF79C6) else Color(0xFFF8F8F2)
                            )
                            IconButton(
                                onClick = { showDeleteConfirm = file },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Excluir",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFF5555).copy(alpha = 0.7f) // Dracula Red
                                )
                            }
                        }
                    }
                }
            }

            // Git push panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = (0.5).dp, color = Color(0xFF282A36)),
                color = Color(0xFF0C0D12)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFBD93F9) // Dracula Purple
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "GIT SYNC",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBD93F9),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mensagem do commit...", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF6272A4))) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFF8F8F2)),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF000000),
                            unfocusedContainerColor = Color(0xFF000000),
                            focusedBorderColor = Color(0xFFFF79C6),
                            unfocusedBorderColor = Color(0xFF282A36),
                            cursorColor = Color(0xFFFF79C6)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (commitMessage.isNotBlank()) {
                                viewModel.commitAndPush(commitMessage.trim())
                                commitMessage = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPushing && commitMessage.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFBD93F9),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1D1F2B),
                            disabledContentColor = Color(0xFF6272A4)
                        )
                    ) {
                        if (isPushing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enviando...", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        } else {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Commit & Push", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // Code editor
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF000000))
        ) {
            if (selectedFile != null) {
                val isHtmlFile = selectedFile!!.endsWith(".html") || selectedFile!!.endsWith(".htm")

                // Editor header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = (0.5).dp, color = Color(0xFF282A36)),
                    color = Color(0xFF0C0D12)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF79C6) // Dracula Pink
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedFile!!,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF79C6),
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HTML Play visualizer button
                            if (isHtmlFile) {
                                Button(
                                    onClick = { showHtmlPreview = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1D1F2B),
                                        contentColor = Color(0xFFA6E22E)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA6E22E)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play HTML", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Visualizar", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                }
                            }

                            // Save button
                            Button(
                                onClick = { viewModel.saveFile() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1D1F2B),
                                    contentColor = Color(0xFFA6E22E)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282A36)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Salvar", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Editor Text Field (with Line Numbers!)
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF000000))
                ) {
                    // 1. Line numbers column
                    val lineCount = fileContent.count { it == '\n' } + 1
                    val lineNumbersText = remember(lineCount) {
                        (1..lineCount).joinToString("\n")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(44.dp)
                            .background(Color(0xFF07080C)) // Pitch black console sidebar
                            .border(width = (0.5).dp, color = Color(0xFF282A36))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = lineNumbersText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFF6272A4).copy(alpha = 0.55f),
                                lineHeight = 20.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Right
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(verticalScrollState)
                                .padding(end = 8.dp)
                        )
                    }

                    // 2. Editor input field
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = fileContent,
                            onValueChange = { viewModel.updateFileContent(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFF8F8F2),
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF79C6)), // Dracula Pink cursor!
                            visualTransformation = DraculaSyntaxTransformation(),
                            decorationBox = { innerTextField ->
                                if (fileContent.isEmpty()) {
                                    Text(
                                        "// Escreva seu código aqui...",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = Color(0xFF6272A4).copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF6272A4).copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Selecione um arquivo para editar",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFF6272A4)
                            )
                        )
                    }
                }
            }

            // Git log terminal console
            AnimatedVisibility(
                visible = gitLog != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                gitLog?.let { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .border(width = (0.5).dp, color = Color(0xFF282A36)),
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp), // geometric clean edges
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0C0D12)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            Text(
                                "GIT CONSOLE",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6272A4)
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = log,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFFA6E22E) // Draco Green
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // HTML Live Preview Dialog
    if (showHtmlPreview && selectedFile != null) {
        HtmlPreviewDialog(
            htmlCode = fileContent,
            onDismiss = { showHtmlPreview = false }
        )
    }

    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = {
                Text(
                    "Novo Arquivo",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8F8F2)
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Caminho do arquivo", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)) },
                    placeholder = { Text("ex: src/main.kt", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF6272A4))) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFF8F8F2)),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF79C6),
                        unfocusedBorderColor = Color(0xFF282A36),
                        cursorColor = Color(0xFFFF79C6)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createFile(newFileName.trim())
                            newFileName = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6E22E), contentColor = Color.Black)
                ) {
                    Text("Criar", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancelar", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFFF5555)))
                }
            },
            containerColor = Color(0xFF0C0D12),
            shape = RoundedCornerShape(4.dp)
        )
    }

    // Delete confirmation
    showDeleteConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = {
                Text(
                    "Excluir arquivo?",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5555)
                    )
                )
            },
            text = {
                Text(
                    "Deseja excluir permanentemente o arquivo \"$file\"?",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFF8F8F2)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5555),
                        contentColor = Color.White
                    )
                ) {
                    Text("Excluir", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF6272A4)))
                }
            },
            containerColor = Color(0xFF0C0D12),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
private fun HtmlPreviewDialog(
    htmlCode: String,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false // full screen dialog
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000) // Pure AMOLED black background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0D12))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFA6E22E),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "HTML PREVIEW",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8F8F2)
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color(0xFFFF5555))
                    }
                }

                // WebView
                val context = androidx.compose.ui.platform.LocalContext.current
                val webView = remember {
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = android.webkit.WebViewClient()
                    }
                }

                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { webView },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    update = { view ->
                        view.loadDataWithBaseURL(null, htmlCode, "text/html", "UTF-8", null)
                    }
                )
            }
        }
    }
}
