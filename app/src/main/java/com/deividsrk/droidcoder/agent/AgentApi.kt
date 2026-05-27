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

    @Volatile
    private var verifiedBaseUrl: String? = null
    @Volatile
    private var lastCheckedBaseUrl: String? = null

    private fun getBaseUrlToUse(config: AppConfig): String {
        val originalBase = config.apiBaseUrl.trimEnd('/')
        synchronized(this) {
            if (lastCheckedBaseUrl == originalBase && verifiedBaseUrl != null) {
                return verifiedBaseUrl!!
            }
        }
        return originalBase
    }

    /**
     * Fetch available models from the provider's /models endpoint.
     */
    suspend fun fetchModels(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val config = getConfig()
            val originalBase = config.apiBaseUrl.trimEnd('/')

            // Try different candidates
            val candidates = mutableListOf<String>()
            candidates.add(originalBase)
            if (!originalBase.contains("/v1") && 
                !originalBase.contains("googleapis.com") && 
                !originalBase.contains("openai.com") &&
                !originalBase.contains("anthropic.com")) {
                candidates.add("$originalBase/v1")
            }

            var lastException: Exception? = null
            var successBaseUrl: String? = null
            var modelList: ModelListResponse? = null

            for (base in candidates) {
                try {
                    val request = Request.Builder()
                        .url("$base/models")
                        .header("Authorization", "Bearer ${config.apiKey}")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: continue

                    if (response.isSuccessful) {
                        modelList = json.decodeFromString<ModelListResponse>(body)
                        successBaseUrl = base
                        break
                    } else {
                        lastException = IOException("HTTP ${response.code}: $body")
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            if (successBaseUrl == null || modelList == null) {
                return@withContext Result.failure(
                    lastException ?: IOException("Não foi possível buscar os modelos dos servidores candidatos.")
                )
            }

            synchronized(this@AgentApi) {
                lastCheckedBaseUrl = originalBase
                verifiedBaseUrl = successBaseUrl
            }

            // Return all model ids to show them correctly to the user
            val chatModels = modelList.data.map { it.id }

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
            val originalBase = config.apiBaseUrl.trimEnd('/')
            val baseUrl = getBaseUrlToUse(config)

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

            val bodyStr = requestBody.toString()
            var request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(bodyStr.toRequestBody(jsonMediaType))
                .build()

            var response = client.newCall(request).execute()
            var responseBody = response.body?.string() ?: ""

            // Fallback for chat completions if not verified yet and /v1 might be missing
            if (!response.isSuccessful && verifiedBaseUrl == null &&
                !originalBase.contains("/v1") &&
                !originalBase.contains("googleapis.com") &&
                !originalBase.contains("openai.com") &&
                !originalBase.contains("anthropic.com")) {

                val fallbackBase = "$originalBase/v1"
                val fallbackRequest = Request.Builder()
                    .url("$fallbackBase/chat/completions")
                    .header("Authorization", "Bearer ${config.apiKey}")
                    .header("Content-Type", "application/json")
                    .post(bodyStr.toRequestBody(jsonMediaType))
                    .build()

                try {
                    val fallbackResponse = client.newCall(fallbackRequest).execute()
                    val fallbackBody = fallbackResponse.body?.string() ?: ""
                    if (fallbackResponse.isSuccessful) {
                        response = fallbackResponse
                        responseBody = fallbackBody
                        synchronized(this@AgentApi) {
                            lastCheckedBaseUrl = originalBase
                            verifiedBaseUrl = fallbackBase
                        }
                    }
                } catch (e: Exception) {
                    // Ignore fallback failure
                }
            }

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
                    val argsElement = tc.function.arguments
                    when (argsElement) {
                        is JsonObject -> {
                            argsElement.mapValues { (_, v) ->
                                if (v is JsonPrimitive) v.content else v.toString()
                            }
                        }
                        is JsonPrimitive -> {
                            if (argsElement.isString) {
                                val parsed = json.parseToJsonElement(argsElement.content)
                                if (parsed is JsonObject) {
                                    parsed.mapValues { (_, v) -> v.jsonPrimitive.content }
                                } else {
                                    emptyMap()
                                }
                            } else {
                                emptyMap()
                            }
                        }
                        else -> emptyMap()
                    }
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
