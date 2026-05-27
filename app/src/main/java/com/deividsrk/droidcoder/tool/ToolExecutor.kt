package com.deividsrk.droidcoder.tool

import com.deividsrk.droidcoder.agent.ToolCallFunction
import com.deividsrk.droidcoder.file.FileManager
import com.deividsrk.droidcoder.git.GitManager
import kotlinx.serialization.json.*

/**
 * Executes tool calls returned by the AI model.
 * Each tool is dispatched to the appropriate handler.
 */
object ToolExecutor {

    suspend fun execute(
        toolCall: ToolCallFunction,
        fileManager: FileManager,
        gitManager: GitManager
    ): String {
        val args: Map<String, String> = try {
            val argsElement = toolCall.arguments
            when (argsElement) {
                is JsonObject -> {
                    argsElement.mapValues { (_, v) ->
                        if (v is JsonPrimitive) v.content else v.toString()
                    }
                }
                is JsonPrimitive -> {
                    if (argsElement.isString) {
                        val parsed = Json.parseToJsonElement(argsElement.content)
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

        return try {
            when (toolCall.name) {
                "list_files" -> executeListFiles(fileManager)
                "read_file" -> executeReadFile(fileManager, args)
                "write_file" -> executeWriteFile(fileManager, args)
                "delete_file" -> executeDeleteFile(fileManager, args)
                "edit_file" -> executeEditFile(fileManager, args)
                "search_grep" -> executeSearchGrep(fileManager, args)
                "count_stats" -> executeCountStats(fileManager, args)
                "web_fetch" -> executeWebFetch(args)
                "git_status" -> executeGitStatus(gitManager)
                "git_commit" -> executeGitCommit(gitManager, args)
                "git_push" -> executeGitPush(gitManager)
                "git_clone" -> executeGitClone(gitManager, args)
                "finish" -> "Tarefa finalizada com sucesso."
                else -> "Erro: Ferramenta desconhecida \"${toolCall.name}\"."
            }
        } catch (e: Exception) {
            "Erro ao executar \"${toolCall.name}\": ${e.message}"
        }
    }

    private suspend fun executeListFiles(fm: FileManager): String {
        val files = fm.listFiles()
        if (files.isEmpty()) return "Nenhum arquivo encontrado no projeto."
        return "📁 Arquivos do projeto (${files.size} arquivos):\n${files.joinToString("\n") { "  • $it" }}"
    }

    private suspend fun executeReadFile(fm: FileManager, args: Map<String, String>): String {
        val path = args["path"] ?: throw IllegalArgumentException("Caminho do arquivo não especificado.")
        val content = fm.readFile(path)
        return "📄 Conteúdo de \"$path\":\n```\n$content\n```"
    }

    private suspend fun executeWriteFile(fm: FileManager, args: Map<String, String>): String {
        val path = args["path"] ?: throw IllegalArgumentException("Caminho não especificado.")
        val content = args["content"] ?: throw IllegalArgumentException("Conteúdo não especificado.")
        fm.writeFile(path, content)
        return "✅ Arquivo \"$path\" criado/editado com sucesso!"
    }

    private suspend fun executeDeleteFile(fm: FileManager, args: Map<String, String>): String {
        val path = args["path"] ?: throw IllegalArgumentException("Caminho não especificado.")
        fm.deleteFile(path)
        return "🗑️ Arquivo \"$path\" excluído com sucesso!"
    }

    private suspend fun executeGitStatus(gm: GitManager): String {
        val status = gm.getStatus()
        if (status.isBlank()) return "✅ Repositório limpo. Nenhuma alteração pendente."
        return "📋 Status do Git:\n$status"
    }

    private suspend fun executeGitCommit(gm: GitManager, args: Map<String, String>): String {
        val message = args["message"] ?: throw IllegalArgumentException("Mensagem de commit não especificada.")
        val sha = gm.commit(message)
        return "✅ Commit efetuado! SHA: $sha"
    }

    private suspend fun executeGitPush(gm: GitManager): String {
        gm.push()
        return "✅ Push concluído! Código enviado ao GitHub."
    }

    private suspend fun executeGitClone(gm: GitManager, args: Map<String, String>): String {
        val url = args["url"] ?: throw IllegalArgumentException("URL do repositório não especificada.")
        val branch = args["branch"] ?: "main"
        gm.clone(url, branch)
        return "✅ Repositório clonado com sucesso de $url (branch: $branch)"
    }

    private suspend fun executeEditFile(fm: FileManager, args: Map<String, String>): String {
        val path = args["path"] ?: throw IllegalArgumentException("Caminho do arquivo não especificado.")
        val target = args["target"] ?: throw IllegalArgumentException("Texto alvo não especificado.")
        val replacement = args["replacement"] ?: throw IllegalArgumentException("Texto de substituição não especificado.")

        val content = fm.readFile(path)
        if (!content.contains(target)) {
            return "Erro: O texto alvo ('target') não foi encontrado no arquivo \"$path\". Certifique-se de que a quebra de linha e os espaços correspondem exatamente."
        }
        
        val occurrences = content.split(target).size - 1
        if (occurrences > 1) {
            return "Erro: O texto alvo ('target') ocorre $occurrences vezes no arquivo \"$path\". Ele deve ser único para garantir uma substituição precisa."
        }

        val newContent = content.replace(target, replacement)
        fm.writeFile(path, newContent)
        
        // Compute Myers diff via native C++ if available
        val diff = if (com.deividsrk.droidcoder.file.NativeBridge.isNativeAvailable) {
            com.deividsrk.droidcoder.file.NativeBridge.computeDiff(target, replacement)
        } else {
            "-$target\n+$replacement"
        }
        
        return "✅ Arquivo \"$path\" editado com sucesso!\n\nDiff da substituição:\n```diff\n$diff\n```"
    }

    private suspend fun executeSearchGrep(fm: FileManager, args: Map<String, String>): String {
        val query = args["query"] ?: throw IllegalArgumentException("Termo de busca ('query') não especificado.")
        val extension = args["extension"]?.trim()?.removePrefix(".")

        if (!com.deividsrk.droidcoder.file.NativeBridge.isNativeAvailable) {
            return "Erro: O módulo de busca nativa C++ Boyer-Moore não está disponível."
        }

        val allFiles = fm.listFiles()
        val results = mutableListOf<String>()

        allFiles.forEach { filePath ->
            if (extension == null || filePath.endsWith(".$extension", ignoreCase = true)) {
                try {
                    val content = fm.readFile(filePath)
                    // Call Boyer-Moore fast search in C++
                    val offset = com.deividsrk.droidcoder.file.NativeBridge.fastSearch(content, query)
                    if (offset != -1) {
                        results.add(filePath)
                    }
                } catch (e: Exception) {
                    // Skip unreadable / binary files
                }
            }
        }

        if (results.isEmpty()) return "Nenhuma ocorrência encontrada para \"$query\" no projeto."
        return "🔍 Termo \"$query\" encontrado em ${results.size} arquivos:\n${results.joinToString("\n") { "  • $it" }}"
    }

    private suspend fun executeCountStats(fm: FileManager, args: Map<String, String>): String {
        val path = args["path"] ?: throw IllegalArgumentException("Caminho do arquivo não especificado.")
        val content = fm.readFile(path)

        val stats = if (com.deividsrk.droidcoder.file.NativeBridge.isNativeAvailable) {
            com.deividsrk.droidcoder.file.NativeBridge.countStats(content)
        } else {
            com.deividsrk.droidcoder.file.NativeBridge.countStatsFallback(content)
        }

        val parts = stats.split(":")
        val lines = parts.getOrNull(0) ?: "0"
        val words = parts.getOrNull(1) ?: "0"
        val chars = parts.getOrNull(2) ?: "0"

        return "📊 Estatísticas nativas para \"$path\":\n" +
                "  • Linhas: $lines\n" +
                "  • Palavras: $words\n" +
                "  • Caracteres: $chars"
    }

    private suspend fun executeWebFetch(args: Map<String, String>): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val url = args["url"] ?: throw IllegalArgumentException("URL não especificada.")
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "Erro: Falha na requisição HTTP (Código: ${response.code})"
                val body = response.body?.string() ?: return@withContext "Erro: Resposta vazia da URL."
                
                // Extract plain text from HTML (extremely simple extraction for size and readability)
                val cleanText = body
                    .replace(Regex("<script[\\s\\S]*?</script>"), "")
                    .replace(Regex("<style[\\s\\S]*?</style>"), "")
                    .replace(Regex("<[^>]*>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                
                val limit = 8000
                val truncated = if (cleanText.length > limit) cleanText.take(limit) + "\n... [conteúdo truncado]" else cleanText
                "🌐 Conteúdo baixado de \"$url\":\n\n$truncated"
            }
        } catch (e: Exception) {
            "Erro ao fazer fetch de \"$url\": ${e.message}"
        }
    }
}
