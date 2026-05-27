package com.deividsrk.droidcoder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deividsrk.droidcoder.ui.MainViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val gitLog by viewModel.gitLog.collectAsStateWithLifecycle()
    val isPushing by viewModel.isPushing.collectAsStateWithLifecycle()

    var apiBaseUrl by remember { mutableStateOf(config.apiBaseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var selectedModel by remember { mutableStateOf(config.model) }
    var temperature by remember { mutableStateOf(config.temperature.toString()) }
    var githubToken by remember { mutableStateOf(config.githubToken) }
    var repoUrl by remember { mutableStateOf(config.repoUrl) }
    var authorName by remember { mutableStateOf(config.authorName) }
    var authorEmail by remember { mutableStateOf(config.authorEmail) }
    var showApiKey by remember { mutableStateOf(false) }
    var showGithubToken by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var cloneUrl by remember { mutableStateOf("") }
    var cloneBranch by remember { mutableStateOf("main") }

    val context = LocalContext.current
    var useForegroundService by remember { mutableStateOf(config.useForegroundService) }
    var alwaysShowThought by remember { mutableStateOf(config.alwaysShowThought) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            useForegroundService = true
        } else {
            useForegroundService = false
            Toast.makeText(context, "Permissão de notificação negada. O progresso em segundo plano não será visível.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(config) {
        apiBaseUrl = config.apiBaseUrl
        apiKey = config.apiKey
        selectedModel = config.model
        temperature = config.temperature.toString()
        githubToken = config.githubToken
        repoUrl = config.repoUrl
        authorName = config.authorName
        authorEmail = config.authorEmail
        useForegroundService = config.useForegroundService
        alwaysShowThought = config.alwaysShowThought
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Configurações",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ---- AI Provider Section ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Provedor de IA", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                }

                // API Base URL
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp)
                )

                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-... ou chave do Gemini") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    visualTransformation = if (showApiKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                // Model selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Modelo") },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(4.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            if (availableModels.isEmpty() && !isLoadingModels) {
                                DropdownMenuItem(
                                    text = { Text("gpt-4o (padrão)") },
                                    onClick = { selectedModel = "gpt-4o"; modelDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("gpt-4o-mini") },
                                    onClick = { selectedModel = "gpt-4o-mini"; modelDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("gpt-4-turbo") },
                                    onClick = { selectedModel = "gpt-4-turbo"; modelDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("gpt-3.5-turbo") },
                                    onClick = { selectedModel = "gpt-3.5-turbo"; modelDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("gemini-2.0-flash") },
                                    onClick = { selectedModel = "gemini-2.0-flash"; modelDropdownExpanded = false }
                                )
                            } else {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = { selectedModel = model; modelDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.updateConfig(
                                config.copy(
                                    apiBaseUrl = apiBaseUrl,
                                    apiKey = apiKey,
                                    temperature = temperature.toDoubleOrNull() ?: 0.3,
                                    githubToken = githubToken,
                                    repoUrl = repoUrl,
                                    authorName = authorName,
                                    authorEmail = authorEmail,
                                    alwaysShowThought = alwaysShowThought
                                )
                            )
                            viewModel.fetchModels()
                        },
                        enabled = !isLoadingModels
                    ) {
                        if (isLoadingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Buscar")
                    }
                }

                // Temperature
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (0.0 - 1.0)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }

        // ---- GitHub Section ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GitHub", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                }

                // GitHub Token
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = { githubToken = it },
                    label = { Text("Personal Access Token (PAT)") },
                    placeholder = { Text("ghp_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    visualTransformation = if (showGithubToken) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGithubToken = !showGithubToken }) {
                            Icon(
                                if (showGithubToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                // Repo URL
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("URL do Repositório") },
                    placeholder = { Text("https://github.com/usuario/projeto.git") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp)
                )

                // Author info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Nome do Autor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp)
                    )
                    OutlinedTextField(
                        value = authorEmail,
                        onValueChange = { authorEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }

        // ---- Clone Repository Section ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clonar Repositório", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cloneUrl,
                        onValueChange = { cloneUrl = it },
                        label = { Text("URL do repositório") },
                        placeholder = { Text("https://github.com/...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp)
                    )
                    OutlinedTextField(
                        value = cloneBranch,
                        onValueChange = { cloneBranch = it },
                        label = { Text("Branch") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp)
                    )
                }
                Button(
                    onClick = { if (cloneUrl.isNotBlank()) viewModel.cloneRepository(cloneUrl, cloneBranch) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPushing && cloneUrl.isNotBlank(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (isPushing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPushing) "Clonando..." else "Clonar")
                }

                if (gitLog != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = gitLog!!,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ---- Foreground Service Section ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Serviço em Segundo Plano", 
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = useForegroundService,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (!hasPermission) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        useForegroundService = true
                                    }
                                } else {
                                    useForegroundService = true
                                }
                            } else {
                                useForegroundService = false
                            }
                        }
                    )
                }
                Text(
                    text = "Executa as tarefas da IA em segundo plano através de um serviço do Android com notificações. Mostra o progresso e as ferramentas executadas em tempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Thought Visuals Section ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pensamento da IA", 
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = alwaysShowThought,
                        onCheckedChange = { alwaysShowThought = it }
                    )
                }
                Text(
                    text = "Mostra o raciocínio interno/pensamento do agente automaticamente expandido por padrão na tela de chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Save Button ----
        Button(
            onClick = {
                viewModel.updateConfig(
                    config.copy(
                        apiBaseUrl = apiBaseUrl,
                        apiKey = apiKey,
                        model = selectedModel,
                        temperature = temperature.toDoubleOrNull() ?: 0.3,
                        githubToken = githubToken,
                        repoUrl = repoUrl,
                        authorName = authorName,
                        authorEmail = authorEmail,
                        useForegroundService = useForegroundService,
                        alwaysShowThought = alwaysShowThought
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Salvar Configurações", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(80.dp))
    }
}
