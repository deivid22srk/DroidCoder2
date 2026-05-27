package com.deividsrk.droidcoder.agent

import com.deividsrk.droidcoder.tool.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Handles communication with OpenAI-compatible APIs.
 * Supports OpenAI, Gemini, and any OpenAI-compatible provider.
 * Auto-fetches available models from the API.
 */
class AgentApi(
    private val getConfig: () -> AppConfig
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetch available models from the provider's /models endpoint.
     */
    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val config = getConfig()
            val baseUrl = config.apiBaseUrl.trimEnd('/')

            val request = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer ${config.apiKey}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Resposta vazia do servidor")
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: $body")
                )
            }

            val modelList = json.decodeFromString<ModelListResponse>(body)
            val chatModels = modelList.data
                .filter { it.id.contains("gpt", true) || it.id.contains("gemini", true) ||
                    it.id.contains("claude", true) || it.id.contains("llama", true) ||
                    it.id.contains("mistral", true) || it.id.contains("command", true) ||
                    it.id.contains("chat", true) }
                .map { it.id }
                .ifEmpty { modelList.data.map { it.id } }

            Result.success(chatModels.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a chat completion request to the LLM.
     * Returns the parsed response with thought, tool call, or final content.
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Result<AgentResponse> = withContext(Dispatchers.IO) {
        try {
            val config = getConfig()
            val baseUrl = config.apiBaseUrl.trimEnd('/')

            val openAiMessages = buildOpenAiMessages(messages, systemPrompt)

            val tools = ToolRegistry.ALL_TOOLS.map { json.encodeToJsonElement(it) }
            val requestBody = buildJsonObject {
                put("model", config.model)
                put("messages", json.encodeToJsonElement(openAiMessages))
                put("temperature", config.temperature)
                put("max_tokens", config.maxTokens)
                if (tools.isNotEmpty()) {
                    put("tools", json.encodeToJsonElement(tools))
                    put("tool_choice", "auto")
                }
            }

            val bodyStr = json.encodeToString(JsonObject(requestBody))
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(bodyStr.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: $responseBody")
                )
            }

            val completion = json.decodeFromString<ChatCompletionResponse>(responseBody)

            if (completion.error != null) {
                return@withContext Result.failure(
                    IOException(completion.error.message)
                )
            }

            val choice = completion.choices?.firstOrNull()
                ?: return@withContext Result.failure(IOException("Nenhuma escolha na resposta."))

            val content = choice.message.content ?: ""
            val thought = extractThought(content)

            // Check for tool calls
            val toolCalls = choice.message.tool_calls
            val toolCall = if (!toolCalls.isNullOrEmpty()) {
                val tc = toolCalls.first()
                val args: Map<String, String> = try {
                    val parsed = json.parseToJsonElement(tc.function.arguments).jsonObject
                    parsed.mapValues { (_, v) -> v.jsonPrimitive.content }
                } catch (e: Exception) {
                    emptyMap()
                }
                ToolCallSpec(
                    name = tc.function.name,
                    arguments = args
                )
            } else {
                null
            }

            Result.success(
                AgentResponse(
                    thought = thought,
                    toolCall = toolCall,
                    response = if (toolCall != null) content.ifBlank { "Executando ${toolCall.name}..." }
                    else content.ifBlank { "Tarefa concluída." }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildOpenAiMessages(
        history: List<ChatMessage>,
        systemPrompt: String
    ): List<OpenAiMessage> {
        val messages = mutableListOf<OpenAiMessage>()
        messages.add(OpenAiMessage(role = "system", content = systemPrompt))

        for (msg in history) {
            messages.add(
                OpenAiMessage(
                    role = msg.role,
                    content = msg.content
                )
            )
        }
        return messages
    }

    private fun extractThought(content: String): String? {
        val patterns = listOf(
            Regex("""<thought>(.*?)</thought>""", RegexOption.DOT_MATCHES_ALL),
            Regex("""\"thought\"\s*:\s*\"(.*?)\"""", RegexOption.DOT_MATCHES_ALL),
            Regex("""<thinking>(.*?)</thinking>""", RegexOption.DOT_MATCHES_ALL),
        )
        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) return match.groupValues[1].trim()
        }
        return if (content.length > 200 && content.contains("pensar", true)) {
            content.take(500)
        } else {
            null
        }
    }
}
