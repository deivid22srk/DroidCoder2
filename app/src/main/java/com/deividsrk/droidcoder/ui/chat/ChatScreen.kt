package com.deividsrk.droidcoder.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deividsrk.droidcoder.agent.ChatMessage
import com.deividsrk.droidcoder.ui.MainViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import android.widget.Toast

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isAgentRunning by viewModel.isAgentRunning.collectAsStateWithLifecycle()
    val agentStatus by viewModel.agentStatus.collectAsStateWithLifecycle()
    val agentThought by viewModel.agentThought.collectAsStateWithLifecycle()
    val agentTool by viewModel.agentTool.collectAsStateWithLifecycle()
    val agentToolArgs by viewModel.agentToolArgs.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    var inputText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Agent status bar
        AnimatedVisibility(
            visible = isAgentRunning,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0C0D12) // Dracula AMOLED dark container
                ),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Terminal header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Mac-style window controls
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))
                        }
                        Text(
                            text = "droidcoder-cli v2.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Terminal body
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "droidcoder@agent:~$ status --watch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "status: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00FF00),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = agentStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0E0E0),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (agentTool != null) {
                            Text(
                                text = "[TOOL] Executando ${agentTool}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (agentToolArgs != null) {
                            Text(
                                text = "  ↳ args: { $agentToolArgs }",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB300),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        // Cursor blink line
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "processing",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888),
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(width = 8.dp, height = 12.dp)
                                    .background(Color(0xFFE0E0E0).copy(alpha = cursorAlpha))
                            )
                        }
                    }
                }
            }
        }

        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty() && !isAgentRunning) {
                item {
                    EmptyChatPlaceholder()
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    isUser = msg.role == "user"
                )
            }

            // Live thought display
            if (isAgentRunning && agentThought != null) {
                item {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha) // pulsing pink border
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "PENSANDO EM TEMPO REAL...",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.cancelCurrentTask() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Cancelar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = agentThought!!,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Input area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (isAgentRunning) "O agente está trabalhando..."
                            else "Descreva o que deseja fazer com o código...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    maxLines = 4,
                    enabled = !isAgentRunning,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = !isAgentRunning && inputText.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                    )
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            "Como posso ajudar hoje?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Sou o agente de IA do DroidCoder2. Posso acessar, editar e excluir arquivos, " +
                    "além de enviar código para o GitHub diretamente do seu celular!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isUser: Boolean
) {
    var showThought by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 48.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // Bot avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (message.role == "tool") {
                                Modifier.background(Color(0xFF1E1E1E))
                            } else {
                                Modifier.background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (message.role == "tool") Icons.Filled.Terminal
                        else Icons.Filled.Android,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            if (message.role == "tool") {
                val toolTheme = getToolTheme(message.toolExecuted)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0C0D12) // Dracula AMOLED dark container
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Title/Header Bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Mini circles
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                
                                // Tool Badge
                                Surface(
                                    color = toolTheme.badgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            toolTheme.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = toolTheme.badgeTextColor
                                        )
                                        Text(
                                            text = toolTheme.friendlyName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = toolTheme.badgeTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            
                            // Success indicator
                            Surface(
                                color = Color(0xFF064E3B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SUCESSO",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Beautiful IDE code block displaying code edits / terminal output!
                        IdeCodeBlock(
                            code = message.content,
                            title = message.toolExecuted ?: "resultado_terminal"
                        )
                    }
                }
            } else {
                val hasToolCall = !isUser && message.toolExecuted != null
                val toolTheme = getToolTheme(message.toolExecuted)
                
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (isUser) null else androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Thought (only for assistant)
                        if (!isUser && message.thought != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                color = Color(0xFF07080B), // pitch-black card background for thoughts
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    TextButton(
                                        onClick = { showThought = !showThought },
                                        modifier = Modifier.fillMaxWidth().height(28.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = Color(0xFFA78BFA)
                                            )
                                            Text(
                                                "PENSAMENTO DO AGENTE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA78BFA),
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Icon(
                                                if (showThought) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFFA78BFA)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = showThought) {
                                        Column(modifier = Modifier.padding(top = 6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Color(0xFF2D2344))
                                                    .padding(bottom = 6.dp)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = message.thought!!,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFDDD6FE),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Tool Executed Tag (inline indicator)
                        if (hasToolCall) {
                            Surface(
                                color = toolTheme.badgeColor,
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        toolTheme.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = toolTheme.badgeTextColor
                                    )
                                    Text(
                                        text = "AÇÃO ACIONADA: ${toolTheme.friendlyName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = toolTheme.badgeTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Content
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isUser) {
                Spacer(Modifier.width(8.dp))
                // User avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ---- Visual Redesign Helper Components ----

data class ToolTheme(
    val friendlyName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeColor: Color,
    val badgeTextColor: Color
)

@Composable
private fun getToolTheme(toolName: String?): ToolTheme {
    return when (toolName?.lowercase()) {
        "write_file", "create_file" -> ToolTheme(
            friendlyName = "Escrevendo Arquivo",
            icon = Icons.Outlined.Code,
            badgeColor = Color(0xFF064E3B), // emerald container
            badgeTextColor = Color(0xFFA7F3D0)
        )
        "read_file", "view_file" -> ToolTheme(
            friendlyName = "Lendo Arquivo",
            icon = Icons.Outlined.Article,
            badgeColor = Color(0xFF78350F), // amber container
            badgeTextColor = Color(0xFFFDE68A)
        )
        "list_files", "find_files" -> ToolTheme(
            friendlyName = "Listando Arquivos",
            icon = Icons.Outlined.FolderOpen,
            badgeColor = Color(0xFF1E3A8A), // indigo container
            badgeTextColor = Color(0xFFBFDBFE)
        )
        "delete_file" -> ToolTheme(
            friendlyName = "Excluindo Arquivo",
            icon = Icons.Outlined.Delete,
            badgeColor = Color(0xFF7F1D1D), // red container
            badgeTextColor = Color(0xFFFCA5A5)
        )
        "git_commit", "git_push", "commitandpush" -> ToolTheme(
            friendlyName = "Git Operação",
            icon = Icons.Outlined.CloudUpload,
            badgeColor = Color(0xFF4C1D95), // violet container
            badgeTextColor = Color(0xFFDDD6FE)
        )
        "git_clone", "clone" -> ToolTheme(
            friendlyName = "Clonando Repositório",
            icon = Icons.Outlined.CloudDownload,
            badgeColor = Color(0xFF0369A1), // sky container
            badgeTextColor = Color(0xFFBAE6FD)
        )
        "edit_file" -> ToolTheme(
            friendlyName = "Editando Arquivo",
            icon = Icons.Outlined.Edit,
            badgeColor = Color(0xFF0F5A47), // teal container
            badgeTextColor = Color(0xFF91F2D9)
        )
        "search_grep" -> ToolTheme(
            friendlyName = "Pesquisa Boyer-Moore C++",
            icon = Icons.Outlined.Search,
            badgeColor = Color(0xFF831843), // pink container
            badgeTextColor = Color(0xFFFBCFE8)
        )
        "count_stats" -> ToolTheme(
            friendlyName = "Estatísticas C++",
            icon = Icons.Outlined.Analytics,
            badgeColor = Color(0xFF065F46), // green container
            badgeTextColor = Color(0xFFA7F3D0)
        )
        "web_fetch" -> ToolTheme(
            friendlyName = "Navegando na Web",
            icon = Icons.Outlined.Language,
            badgeColor = Color(0xFF075985), // light blue container
            badgeTextColor = Color(0xFFE0F2FE)
        )
        else -> ToolTheme(
            friendlyName = toolName ?: "Executando Ação",
            icon = Icons.Outlined.Build,
            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
            badgeTextColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun IdeCodeBlock(
    code: String,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { code.split("\n") }
    val isLongCode = lines.size > 12
    var isExpanded by remember { mutableStateOf(!isLongCode) }
    val displayLines = if (isExpanded) lines else lines.take(10)

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isHtml = remember(code, title) {
        (title != null && (title.endsWith(".html") || title.contains(".html"))) || 
        code.contains("<!DOCTYPE html>", ignoreCase = true) || 
        code.contains("<html", ignoreCase = true)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF050608) // deep dark IDE bg
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282A36))
    ) {
        Column {
            // Title tab bar simulating IDE header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1F29))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // macOS window dots
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = title ?: "arquivo.kt",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA6ACCD),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isHtml) {
                        var showPreview by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showPreview = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Visualizar HTML",
                                tint = Color(0xFFA6E22E),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showPreview) {
                            HtmlPreviewDialog(
                                htmlCode = code,
                                onDismiss = { showPreview = false }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copiar código",
                            tint = Color(0xFFA6ACCD),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Code lines container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    displayLines.forEachIndexed { index, line ->
                        val isAddition = line.startsWith("+") && !line.startsWith("+++")
                        val isDeletion = line.startsWith("-") && !line.startsWith("---")

                        val bgColor = when {
                            isAddition -> Color(0x1A10B981) // emerald transparent glow
                            isDeletion -> Color(0x1AEF4444) // red transparent glow
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isAddition -> Color(0xFF4ADE80)
                            isDeletion -> Color(0xFFF87171)
                            else -> Color(0xFFE2E8F0)
                        }

                        val prefix = when {
                            isAddition -> "+"
                            isDeletion -> "-"
                            else -> " "
                        }

                        val cleanLine = if (isAddition || isDeletion) line.drop(1) else line

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Line number
                            Text(
                                text = String.format("%3d", index + 1),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF4E5579),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(28.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            // Diff prefix
                            Text(
                                text = prefix,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = textColor.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(12.dp)
                            )
                            // Code text with premium syntax highlighting
                            val highlightedText = remember(cleanLine) { highlightCode(cleanLine) }
                            Text(
                                text = highlightedText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (!isExpanded && isLongCode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF12131A))))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Subtle placeholder for hidden lines
                        }
                    }
                }
            }

            // Expand / Collapse action bar
            if (isLongCode) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF282A36)))
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Ver Menos" else "Ver Mais (${lines.size - 10} linhas ocultas)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun highlightCode(text: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        val length = text.length

        while (index < length) {
            val char = text[index]

            // 1. Comments (Line comments //)
            if (char == '/' && index + 1 < length && text[index + 1] == '/') {
                val commentText = text.substring(index)
                withStyle(SpanStyle(color = Color(0xFF6272A4))) { // Dracula comment gray
                    append(commentText)
                }
                break
            }

            // 2. String Literals
            if (char == '"' || char == '\'') {
                val quote = char
                val start = index
                index++
                var escaped = false
                while (index < length) {
                    val c = text[index]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == quote) {
                        index++
                        break
                    }
                    index++
                }
                val stringVal = text.substring(start, minOf(index, length))
                withStyle(SpanStyle(color = Color(0xFFA6E22E))) { // String green
                    append(stringVal)
                }
                continue
            }

            // 3. Numbers
            if (char.isDigit()) {
                val start = index
                while (index < length && (text[index].isDigit() || text[index] == '.' || text[index] == 'f' || text[index] == 'L')) {
                    index++
                }
                val numVal = text.substring(start, index)
                withStyle(SpanStyle(color = Color(0xFFBD93F9))) { // Number purple
                    append(numVal)
                }
                continue
            }

            // 4. Identifiers (Keywords, Types, Annotations)
            if (char.isLetter() || char == '_' || char == '@') {
                val start = index
                if (text[index] == '@') {
                    index++
                }
                while (index < length && (text[index].isLetterOrDigit() || text[index] == '_')) {
                    index++
                }
                val word = text.substring(start, index)

                val isKeyword = word in setOf(
                    "class", "interface", "fun", "val", "var", "import", "package", "return", 
                    "if", "else", "while", "for", "in", "object", "private", "public", "protected",
                    "internal", "external", "suspend", "null", "true", "false", "this", "super",
                    "override", "const", "companion", "data", "void", "int", "float", "double", 
                    "bool", "char", "include", "define", "struct", "template", "typename"
                )

                val isType = word.isNotEmpty() && word[0].isUpperCase() && !word.startsWith("@")
                val isAnnotation = word.startsWith("@")

                when {
                    isKeyword -> {
                        withStyle(SpanStyle(color = Color(0xFFFF79C6), fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    isType -> {
                        withStyle(SpanStyle(color = Color(0xFF8BE9FD))) {
                            append(word)
                        }
                    }
                    isAnnotation -> {
                        withStyle(SpanStyle(color = Color(0xFFFFB86C))) {
                            append(word)
                        }
                    }
                    else -> {
                        withStyle(SpanStyle(color = Color(0xFFF8F8F2))) {
                            append(word)
                        }
                    }
                }
                continue
            }

            // 5. General characters (Operators / brackets)
            withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                append(char)
            }
            index++
        }
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
            usePlatformDefaultWidth = false // full screen dialog!
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFA6E22E),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Visualização HTML em Tela Cheia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8F8F2)
                        )
                    }
                    
                    // Close button
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFFFF5555),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // WebView container
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            
                            webViewClient = android.webkit.WebViewClient()
                            
                            loadDataWithBaseURL(null, htmlCode, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize().weight(1f)
                )
            }
        }
    }
}
