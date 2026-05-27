package com.deividsrk.droidcoder.agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Message types used in chat history and LLM communication.
 */
@Serializable
data class ChatMessage(
    val id: String = randomId(),
    val role: String,  // "user", "assistant", "system", "tool"
    val content: String,
    val thought: String? = null,
    val toolExecuted: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Configuration persisted across sessions.
 */
@Serializable
data class AppConfig(
    val apiProvider: String = "openai",           // "openai" or "gemini"
    val apiBaseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o",                  // auto-fetched list
    val temperature: Double = 0.3,
    val maxTokens: Int = 4096,
    val githubToken: String = "",
    val repoUrl: String = "",
    val authorName: String = "DroidCoder2",
    val authorEmail: String = "droidcoder@app.com",
    val projectPath: String = ""                    // folder selected via SAF
)

/**
 * OpenAI-compatible chat completion request.
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 4096,
    val response_format: ResponseFormat? = null,
    val tools: List<ToolDefinition>? = null,
    val tool_choice: String? = null
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCallResponse>? = null,
    val tool_call_id: String? = null
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

/**
 * Tool definition sent to the LLM.
 */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

/**
 * Tool call extracted from LLM response.
 */
@Serializable
data class ToolCallResponse(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String  // JSON string
)

/**
 * Chat completion response.
 */
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val error: ApiError? = null
)

@Serializable
data class Choice(
    val index: Int,
    val message: ChoiceMessage,
    val finish_reason: String? = null
)

@Serializable
data class ChoiceMessage(
    val role: String? = null,
    val content: String? = null,
    val tool_calls: List<ToolCallResponse>? = null
)

@Serializable
data class ApiError(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

/**
 * Model list response from /v1/models.
 */
@Serializable
data class ModelListResponse(
    val `data`: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String,
    val `object`: String = "model",
    val owned_by: String? = null
)

/**
 * Tool result pushed back into the LLM conversation.
 */
@Serializable
data class ToolResult(
    val tool_call_id: String,
    val content: String
)

/**
 * Internal parsed response from the agent.
 */
data class AgentResponse(
    val thought: String? = null,
    val toolCall: ToolCallSpec? = null,
    val response: String = ""
)

@Serializable
data class ToolCallSpec(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

private fun randomId() = (1..12).map {
    "abcdefghijklmnopqrstuvwxyz0123456789".random()
}.joinToString("")
