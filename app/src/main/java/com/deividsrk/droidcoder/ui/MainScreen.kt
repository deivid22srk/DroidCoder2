package com.deividsrk.droidcoder.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deividsrk.droidcoder.ui.chat.ChatScreen
import com.deividsrk.droidcoder.ui.explorer.FileExplorerScreen
import com.deividsrk.droidcoder.ui.settings.SettingsScreen

/**
 * Main screen with bottom navigation:
 * Chat | Files | Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectProjectFolder(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab == 0, // only allow swipe drawer on Chat tab
        drawerContent = {
            if (selectedTab == 0) {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF000000), // AMOLED Black
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp), // Retro-dev square corners!
                    modifier = Modifier.width(280.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title/Header of Drawer
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Chat, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CONVERSAS DE IA", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                        // "+ Nova Conversa" button
                        OutlinedButton(
                            onClick = {
                                viewModel.createNewSession()
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nova Conversa", fontWeight = FontWeight.SemiBold)
                        }

                        // Sessions list
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sessions.size) { index ->
                                val session = sessions[index]
                                val isSelected = session.id == currentSessionId
                                
                                Surface(
                                    onClick = {
                                        viewModel.selectSession(session.id)
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Outlined.ChatBubbleOutline, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete, 
                                                contentDescription = "Deletar conversa", 
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu de conversas",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "DroidCoder",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "2",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        // Folder picker button
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = "Selecionar pasta",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Clear chat
                        if (selectedTab == 0 && messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearChat() }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = "Limpar chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(if (selectedTab == 0) Icons.Filled.Chat else Icons.Outlined.Chat, contentDescription = null) },
                        label = { Text("Chat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(if (selectedTab == 1) Icons.Filled.Code else Icons.Outlined.Code, contentDescription = null) },
                        label = { Text("Arquivos") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("Config") }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> ChatScreen(viewModel = viewModel)
                        1 -> FileExplorerScreen(viewModel = viewModel)
                        2 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
