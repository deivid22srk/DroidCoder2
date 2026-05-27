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

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectProjectFolder(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
                    icon = { Icon(Icons.Outlined.Chat, contentDescription = null) },
                    selectedIcon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    selectedIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
                    label = { Text("Arquivos") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
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
                }
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
