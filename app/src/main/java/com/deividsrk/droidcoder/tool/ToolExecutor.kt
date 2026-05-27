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
            val json = Json.parseToJsonElement(toolCall.arguments).jsonObject
            json.mapValues { (_, v) -> v.jsonPrimitive.content }
        } catch (e: Exception) {
            emptyMap()
        }

        return try {
            when (toolCall.name) {
                "list_files" -> executeListFiles(fileManager)
                "read_file" -> executeReadFile(fileManager, args)
                "write_file" -> executeWriteFile(fileManager, args)
                "delete_file" -> executeDeleteFile(fileManager, args)
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
}
