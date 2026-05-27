package com.deividsrk.droidcoder.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deividsrk.droidcoder.agent.*
import com.deividsrk.droidcoder.services.AgentForegroundService
import com.deividsrk.droidcoder.file.FileManager
import com.deividsrk.droidcoder.git.GitManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Central ViewModel for the DroidCoder2 application.
 *
 * Manages:
 * - App configuration (API key, model, GitHub token, etc.)
 * - Chat messages and AI agent execution
 * - File system state
 * - Git state
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("droidcoder_prefs", Context.MODE_PRIVATE)

    private fun loadConfig(): AppConfig {
        val jsonString = sharedPreferences.getString("app_config", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString<AppConfig>(jsonString)
            } catch (e: Exception) {
                AppConfig()
            }
        } else {
            AppConfig()
        }
    }

    private fun saveConfig(newConfig: AppConfig) {
        try {
            val jsonString = Json.encodeToString(newConfig)
            sharedPreferences.edit().putString("app_config", jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSessions(): List<ChatSession> {
        val jsonString = sharedPreferences.getString("chat_sessions", null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString<List<ChatSession>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun saveSessions(list: List<ChatSession>) {
        try {
            val jsonString = Json.encodeToString(list)
            sharedPreferences.edit().putString("chat_sessions", jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Config
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    // Chat
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Sessions
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private var agentJob: kotlinx.coroutines.Job? = null

    // Agent running state
    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _agentStatus = MutableStateFlow("")
    val agentStatus: StateFlow<String> = _agentStatus.asStateFlow()

    private val _agentThought = MutableStateFlow<String?>(null)
    val agentThought: StateFlow<String?> = _agentThought.asStateFlow()

    private val _agentTool = MutableStateFlow<String?>(null)
    val agentTool: StateFlow<String?> = _agentTool.asStateFlow()

    private val _agentToolArgs = MutableStateFlow<String?>(null)
    val agentToolArgs: StateFlow<String?> = _agentToolArgs.asStateFlow()

    // Files
    private val _files = MutableStateFlow<List<String>>(emptyList())
    val files: StateFlow<List<String>> = _files.asStateFlow()

    private val _selectedFile = MutableStateFlow<String?>(null)
    val selectedFile: StateFlow<String?> = _selectedFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    // Models
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    // Git
    private val _gitLog = MutableStateFlow<String?>(null)
    val gitLog: StateFlow<String?> = _gitLog.asStateFlow()

    private val _isPushing = MutableStateFlow(false)
    val isPushing: StateFlow<Boolean> = _isPushing.asStateFlow()

    val fileManager = FileManager(application)
    val gitManager: GitManager

    private val agentApi: AgentApi
    private val agentCore: AgentCore

    init {
        // Init git manager with config accessors
        gitManager = GitManager(
            context = application,
            getToken = { _config.value.githubToken },
            getRepoUrl = { _config.value.repoUrl },
            getAuthorName = { _config.value.authorName },
            getAuthorEmail = { _config.value.authorEmail }
        )

        agentApi = AgentApi { _config.value }
        agentCore = AgentCore(agentApi, fileManager, gitManager)

        // Initialize project path from persisted config if valid
        val loadedConfig = _config.value
        if (loadedConfig.projectPath.isNotBlank()) {
            fileManager.setProjectRoot(loadedConfig.projectPath)
            gitManager.setProjectRoot(fileManager.projectRoot!!)
            refreshFiles()
        }

        // Initialize chat sessions
        val loadedSessions = loadSessions()
        if (loadedSessions.isEmpty()) {
            val defaultSession = ChatSession(title = "Chat Inicial")
            _sessions.value = listOf(defaultSession)
            _currentSessionId.value = defaultSession.id
            _messages.value = defaultSession.messages
            saveSessions(_sessions.value)
        } else {
            _sessions.value = loadedSessions
            val lastSession = loadedSessions.first()
            _currentSessionId.value = lastSession.id
            _messages.value = lastSession.messages
        }
    }

    /**
     * Update the entire config.
     */
    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
        // Also update git manager with new creds
        if (newConfig.projectPath.isNotBlank()) {
            fileManager.setProjectRoot(newConfig.projectPath)
            gitManager.setProjectRoot(fileManager.projectRoot!!)
        }
    }

    /**
     * Select a project folder via SAF URI.
     */
    fun selectProjectFolder(uri: Uri) {
        fileManager.setProjectRoot(uri)
        val newConfig = _config.value.copy(projectPath = fileManager.projectRoot?.absolutePath ?: "")
        _config.value = newConfig
        saveConfig(newConfig)
        gitManager.setProjectRoot(fileManager.projectRoot!!)
        refreshFiles()
    }

    /**
     * Fetch available models from the configured API base.
     */
    fun fetchModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            agentApi.fetchModels().fold(
                onSuccess = { models ->
                    _availableModels.value = models
                },
                onFailure = { error ->
                    // Fall back to known models
                    _availableModels.value = listOf(
                        "gpt-4o", "gpt-4o-mini", "gpt-4-turbo",
                        "gpt-3.5-turbo", "gpt-4"
                    )
                }
            )
            _isLoadingModels.value = false
        }
    }

    /**
     * Send a user message to the AI agent.
     */
    fun sendMessage(text: String) {
        if (text.isBlank() || _isAgentRunning.value) return

        agentJob = viewModelScope.launch {
            try {
                _isAgentRunning.value = true
                _agentStatus.value = "Iniciando agente..."
                _agentThought.value = null
                _agentTool.value = null
                _agentToolArgs.value = null

                if (_config.value.useForegroundService) {
                    AgentForegroundService.start(getApplication(), "Iniciando agente de IA...")
                }

                val mutableHistory = _messages.value.toMutableList()

                agentCore.executeStep(
                    userMessage = text,
                    history = mutableHistory,
                    config = _config.value
                ) { progress ->
                    _agentStatus.value = progress.status
                    _agentThought.value = progress.thought
                    _agentTool.value = progress.toolName
                    _agentToolArgs.value = progress.toolArgs
                    _messages.value = mutableHistory.toList()
                    updateCurrentSessionMessages(mutableHistory.toList())

                    if (_config.value.useForegroundService) {
                        val statusText = when {
                            !progress.toolName.isNullOrBlank() -> "Executando ferramenta: ${progress.toolName}"
                            !progress.status.isNullOrBlank() -> progress.status
                            else -> "Pensando..."
                        }
                        AgentForegroundService.update(getApplication(), statusText)
                    }
                }.fold(
                    onSuccess = { response ->
                        _messages.value = mutableHistory.toList()
                        updateCurrentSessionMessages(mutableHistory.toList())
                        refreshFiles()
                    },
                    onFailure = { error ->
                        mutableHistory.add(
                            ChatMessage(
                                role = "assistant",
                                content = "❌ Erro: ${error.message}"
                            )
                        )
                        _messages.value = mutableHistory.toList()
                        updateCurrentSessionMessages(mutableHistory.toList())
                    }
                )
            } finally {
                if (_config.value.useForegroundService) {
                    AgentForegroundService.stop(getApplication())
                }
                _isAgentRunning.value = false
                _agentStatus.value = ""
                _agentThought.value = null
                _agentTool.value = null
                _agentToolArgs.value = null
                agentJob = null
            }
        }
    }

    /**
     * Refresh the file list from the project directory.
     */
    fun refreshFiles() {
        viewModelScope.launch {
            try {
                _files.value = fileManager.listFiles()
            } catch (e: Exception) {
                _files.value = emptyList()
            }
        }
    }

    /**
     * Update file content during editing.
     */
    fun updateFileContent(newContent: String) {
        _fileContent.value = newContent
    }

    /**
     * Select a file for editing.
     */
    fun selectFile(path: String) {
        viewModelScope.launch {
            try {
                _selectedFile.value = path
                _fileContent.value = fileManager.readFile(path)
            } catch (e: Exception) {
                // File might have been deleted
                _selectedFile.value = null
                _fileContent.value = ""
            }
        }
    }

    /**
     * Save the currently selected file.
     */
    fun saveFile() {
        val path = _selectedFile.value ?: return
        val content = _fileContent.value
        viewModelScope.launch {
            try {
                fileManager.writeFile(path, content)
                refreshFiles()
            } catch (e: Exception) {
                // Error saving
            }
        }
    }

    /**
     * Delete a file.
     */
    fun deleteFile(path: String) {
        viewModelScope.launch {
            try {
                fileManager.deleteFile(path)
                if (_selectedFile.value == path) {
                    _selectedFile.value = null
                    _fileContent.value = ""
                }
                refreshFiles()
            } catch (e: Exception) {
                // Error deleting
            }
        }
    }

    /**
     * Create a new file.
     */
    fun createFile(path: String, content: String = "// Novo arquivo\n") {
        viewModelScope.launch {
            try {
                fileManager.writeFile(path, content)
                refreshFiles()
            } catch (e: Exception) {
                // Error creating
            }
        }
    }

    /**
     * Clear chat history.
     */
    fun clearChat() {
        cancelCurrentTask()
        _messages.value = emptyList()
        updateCurrentSessionMessages(emptyList())
    }

    private fun updateCurrentSessionMessages(newMessages: List<ChatMessage>) {
        val currentId = _currentSessionId.value ?: return
        val updatedSessions = _sessions.value.map { session ->
            if (session.id == currentId) {
                val newTitle = if (session.title.startsWith("Chat ") || session.title == "Chat Inicial") {
                    val firstUserMsg = newMessages.find { it.role == "user" }?.content ?: session.title
                    if (firstUserMsg.length > 25) firstUserMsg.take(25) + "..." else firstUserMsg
                } else {
                    session.title
                }
                session.copy(messages = newMessages, title = newTitle)
            } else {
                session
            }
        }
        _sessions.value = updatedSessions
        saveSessions(updatedSessions)
    }

    fun selectSession(sessionId: String) {
        cancelCurrentTask()
        val session = _sessions.value.find { it.id == sessionId } ?: return
        _currentSessionId.value = sessionId
        _messages.value = session.messages
    }

    fun createNewSession() {
        cancelCurrentTask()
        val newSession = ChatSession(title = "Chat ${sessions.value.size + 1}")
        val updated = _sessions.value.toMutableList().apply { add(0, newSession) }
        _sessions.value = updated
        _currentSessionId.value = newSession.id
        _messages.value = newSession.messages
        saveSessions(updated)
    }

    fun deleteSession(sessionId: String) {
        if (_sessions.value.size <= 1) {
            val clearedSession = ChatSession(title = "Chat Inicial")
            _sessions.value = listOf(clearedSession)
            _currentSessionId.value = clearedSession.id
            _messages.value = emptyList()
            saveSessions(_sessions.value)
            return
        }
        
        val isDeletingCurrent = _currentSessionId.value == sessionId
        val updated = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updated
        saveSessions(updated)
        
        if (isDeletingCurrent) {
            val nextSession = updated.first()
            _currentSessionId.value = nextSession.id
            _messages.value = nextSession.messages
        }
    }

    fun cancelCurrentTask() {
        if (_isAgentRunning.value) {
            agentJob?.cancel()
            agentJob = null
            _isAgentRunning.value = false
            _agentStatus.value = "Cancelado pelo usuário."
            _agentThought.value = null
            _agentTool.value = null
            _agentToolArgs.value = null
            
            val mutableHistory = _messages.value.toMutableList()
            mutableHistory.add(
                ChatMessage(
                    role = "assistant",
                    content = "⏹️ Tarefa cancelada pelo usuário."
                )
            )
            _messages.value = mutableHistory.toList()
            updateCurrentSessionMessages(mutableHistory.toList())
            
            if (_config.value.useForegroundService) {
                AgentForegroundService.stop(getApplication())
            }
        }
    }

    /**
     * Commit and Push to GitHub.
     */
    fun commitAndPush(message: String) {
        viewModelScope.launch {
            _isPushing.value = true
            _gitLog.value = "Preparando arquivos..."

            try {
                _gitLog.value = _gitLog.value + "\n> Executando commit..."
                val sha = gitManager.commit(message)
                _gitLog.value = _gitLog.value + "\n> Commit efetuado! SHA: $sha"

                _gitLog.value = _gitLog.value + "\n> Enviando para o GitHub (push)..."
                gitManager.push()
                _gitLog.value = _gitLog.value + "\n> ✅ Push concluído com sucesso!"
            } catch (e: Exception) {
                _gitLog.value = _gitLog.value + "\n[ERRO] ${e.message}"
            } finally {
                _isPushing.value = false
            }
        }
    }

    /**
     * Clone a repository.
     */
    fun cloneRepository(url: String, branch: String = "main") {
        viewModelScope.launch {
            _isPushing.value = true
            _gitLog.value = "Iniciando clone..."
            try {
                gitManager.clone(url, branch)
                _gitLog.value = _gitLog.value + "\n> Clone concluído!"
                refreshFiles()
            } catch (e: Exception) {
                _gitLog.value = _gitLog.value + "\n[ERRO] ${e.message}"
            } finally {
                _isPushing.value = false
            }
        }
    }
}
