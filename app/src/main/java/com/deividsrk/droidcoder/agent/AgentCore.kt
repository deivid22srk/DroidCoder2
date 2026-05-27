package com.deividsrk.droidcoder.agent

import com.deividsrk.droidcoder.file.FileManager
import com.deividsrk.droidcoder.git.GitManager
import com.deividsrk.droidcoder.tool.ToolExecutor
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

/**
 * Core agent execution loop.
 *
 * Manages the iterative AI cycle:
 * 1. Send user message + history to LLM
 * 2. Parse response (thought + tool call or final answer)
 * 3. Execute tool calls via ToolExecutor
 * 4. Feed tool results back into conversation
 * 5. Repeat until "finish" or max iterations reached
 */
class AgentCore(
    private val apiClient: AgentApi,
    private val fileManager: FileManager,
    private val gitManager: GitManager
) {
    companion object {
        const val MAX_ITERATIONS = 20
    }

    data class ProgressUpdate(
        val status: String,
        val thought: String? = null,
        val toolName: String? = null,
        val toolArgs: String? = null
    )

    /**
     * Execute one complete agent step: user message → AI → tool execution → response.
     */
    suspend fun executeStep(
        userMessage: String,
        history: MutableList<ChatMessage>,
        config: AppConfig,
        onProgress: suspend (ProgressUpdate) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Add user message
            val userMsg = ChatMessage(
                role = "user",
                content = userMessage
            )
            if (history.isEmpty() || history.last().content != userMessage) {
                history.add(userMsg)
                onProgress(ProgressUpdate("Iniciando agente..."))
            }

            var iterations = 0
            var finalResponse = ""

            while (iterations < MAX_ITERATIONS) {
                iterations++

                onProgress(ProgressUpdate("Pensando (ciclo $iterations)..."))

                val systemPrompt = SystemPrompt.build(fileManager.getProjectName())
                val result = apiClient.sendMessage(history, systemPrompt)

                result.fold(
                     onSuccess = { agentResponse ->
                        val thought = agentResponse.thought
                        val toolCall = agentResponse.toolCall
                        val content = agentResponse.response

                        // Update progress status
                        val status = if (toolCall != null) {
                            "Executando: ${toolCall.name}"
                        } else {
                            "Concluindo..."
                        }
                        val argsFormatted = toolCall?.arguments?.entries?.joinToString(", ") { "${it.key}: \"${it.value}\"" }

                        if (toolCall == null || toolCall.name == "finish") {
                            finalResponse = content
                            // Add assistant message
                            history.add(
                                ChatMessage(
                                    role = "assistant",
                                    content = finalResponse,
                                    thought = thought,
                                    toolExecuted = toolCall?.name
                                )
                            )
                            onProgress(
                                ProgressUpdate(
                                    status = status,
                                    thought = thought,
                                    toolName = toolCall?.name,
                                    toolArgs = argsFormatted
                                )
                            )
                            return@withContext Result.success(finalResponse)
                        }

                        // Add assistant message with the tool call intent
                        history.add(
                            ChatMessage(
                                role = "assistant",
                                content = content,
                                thought = thought,
                                toolExecuted = toolCall.name
                            )
                        )

                        onProgress(
                            ProgressUpdate(
                                status = status,
                                thought = thought,
                                toolName = toolCall.name,
                                toolArgs = argsFormatted
                            )
                        )

                        // Execute the tool
                        val argsJsonObject = kotlinx.serialization.json.buildJsonObject {
                            toolCall.arguments.forEach { (k, v) -> put(k, v) }
                        }
                        val toolResult = ToolExecutor.execute(
                            toolCall = ToolCallFunction(
                                name = toolCall.name,
                                arguments = argsJsonObject
                            ),
                            fileManager = fileManager,
                            gitManager = gitManager
                        )

                        // Feed tool result back
                        history.add(
                            ChatMessage(
                                role = "tool",
                                content = toolResult,
                                toolExecuted = toolCall.name
                            )
                        )

                        onProgress(
                            ProgressUpdate(
                                status = "Resultado obtido, continuando...",
                                thought = null,
                                toolName = null,
                                toolArgs = null
                            )
                        )
                    },
                    onFailure = { error ->
                        return@withContext Result.failure(error)
                    }
                )
            }

            if (finalResponse.isBlank()) {
                finalResponse = "Atingi o limite de iterações. Resumindo o que foi feito até agora."
            }
            history.add(
                ChatMessage(
                    role = "assistant",
                    content = finalResponse
                )
            )
            onProgress(
                ProgressUpdate(
                    status = "Concluído",
                    thought = null,
                    toolName = null,
                    toolArgs = null
                )
            )
            Result.success(finalResponse)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
