package com.deividsrk.droidcoder.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deividsrk.droidcoder.agent.*
import com.deividsrk.droidcoder.file.FileManager
import com.deividsrk.droidcoder.git.GitManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // Config
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    // Chat
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

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
    }

    /**
     * Update the entire config.
     */
    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
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
        _config.value = _config.value.copy(projectPath = fileManager.projectRoot?.absolutePath ?: "")
        gitManager.setProjectRoot(fileManager.projectRoot!!)
        refreshFiles()
    }

    /**
     * Fetch available models from the configured API base.
     */
    fun fetchModels() {
        if (_config.value.apiKey.isBlank()) return

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

        viewModelScope.launch {
            _isAgentRunning.value = true
            _agentStatus.value = "Iniciando agente..."
            _agentThought.value = null
            _agentTool.value = null
            _agentToolArgs.value = null

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
            }.fold(
                onSuccess = { response ->
                    _messages.value = mutableHistory.toList()
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
                }
            )

            _isAgentRunning.value = false
            _agentStatus.value = ""
            _agentThought.value = null
            _agentTool.value = null
            _agentToolArgs.value = null
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
        _messages.value = emptyList()
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
