package com.deividsrk.droidcoder.tool

import kotlinx.serialization.json.*

/**
 * Returns the full list of tool definitions (OpenAI function-calling format)
 * that the AI agent can use.
 */
object ToolRegistry {

    val ALL_TOOLS: List<JsonObject> = listOf(
        tool(
            name = "list_files",
            description = "Lista recursivamente todos os arquivos no diretório do projeto.",
            params = jsonObjectOf()
        ),
        tool(
            name = "read_file",
            description = "Lê o conteúdo completo de um arquivo específico do projeto.",
            params = jsonObjectOf(
                "path" to jsonObject(
                    "type" to "string",
                    "description" to "Caminho relativo do arquivo dentro do projeto."
                )
            ),
            required = listOf("path")
        ),
        tool(
            name = "write_file",
            description = "Cria um novo arquivo ou sobrescreve um arquivo existente com o conteúdo fornecido.",
            params = jsonObjectOf(
                "path" to jsonObject(
                    "type" to "string",
                    "description" to "Caminho relativo do arquivo a ser criado/editado."
                ),
                "content" to jsonObject(
                    "type" to "string",
                    "description" to "Conteúdo completo do arquivo. Deve ser código funcional, nunca placeholders."
                )
            ),
            required = listOf("path", "content")
        ),
        tool(
            name = "delete_file",
            description = "Exclui permanentemente um arquivo do projeto.",
            params = jsonObjectOf(
                "path" to jsonObject(
                    "type" to "string",
                    "description" to "Caminho relativo do arquivo a ser excluído."
                )
            ),
            required = listOf("path")
        ),
        tool(
            name = "git_status",
            description = "Mostra o status atual do repositório Git (arquivos modificados, não rastreados, staged).",
            params = jsonObjectOf()
        ),
        tool(
            name = "git_commit",
            description = "Faz commit de todas as alterações no repositório Git local.",
            params = jsonObjectOf(
                "message" to jsonObject(
                    "type" to "string",
                    "description" to "Mensagem de commit descritiva seguindo conventional commits (ex: 'feat: adiciona tela de login')."
                )
            ),
            required = listOf("message")
        ),
        tool(
            name = "git_push",
            description = "Envia os commits locais para o repositório remoto no GitHub.",
            params = jsonObjectOf()
        ),
        tool(
            name = "git_clone",
            description = "Clona um repositório Git remoto para o diretório do projeto.",
            params = jsonObjectOf(
                "url" to jsonObject(
                    "type" to "string",
                    "description" to "URL do repositório Git (ex: https://github.com/usuario/repo.git)."
                ),
                "branch" to jsonObject(
                    "type" to "string",
                    "description" to "Branch a ser clonada (padrão: main)."
                )
            ),
            required = listOf("url")
        ),
        tool(
            name = "edit_file",
            description = "Faz uma substituição precisa de um bloco de texto específico por um novo texto dentro de um arquivo existente. Use esta ferramenta preferencialmente em vez de write_file para pequenas modificações.",
            params = jsonObjectOf(
                "path" to jsonObject(
                    "type" to "string",
                    "description" to "Caminho relativo do arquivo no projeto."
                ),
                "target" to jsonObject(
                    "type" to "string",
                    "description" to "O bloco exato de texto a ser substituído. Deve corresponder perfeitamente, caractere por caractere (incluindo espaços e quebras de linha)."
                ),
                "replacement" to jsonObject(
                    "type" to "string",
                    "description" to "O novo bloco de texto que substituirá o texto alvo."
                )
            ),
            required = listOf("path", "target", "replacement")
        ),
        tool(
            name = "search_grep",
            description = "Pesquisa recursiva rápida por um padrão de texto ou palavra-chave dentro de todos os arquivos de texto do projeto usando o algoritmo nativo Boyer-Moore-Horspool em C++.",
            params = jsonObjectOf(
                "query" to jsonObject(
                    "type" to "string",
                    "description" to "O texto ou palavra-chave a ser pesquisada."
                ),
                "extension" to jsonObject(
                    "type" to "string",
                    "description" to "Opcional: Filtrar a pesquisa por extensão de arquivo (ex: 'kt', 'cpp', 'xml')."
                )
            ),
            required = listOf("query")
        ),
        tool(
            name = "count_stats",
            description = "Analisa estatísticas textuais avançadas de um arquivo específico (contagem de linhas, palavras e caracteres) processadas nativamente em C++ de alta performance.",
            params = jsonObjectOf(
                "path" to jsonObject(
                    "type" to "string",
                    "description" to "Caminho relativo do arquivo no projeto."
                )
            ),
            required = listOf("path")
        ),
        tool(
            name = "web_fetch",
            description = "Baixa e extrai o conteúdo de texto legível de qualquer URL da web (útil para ler documentações externas ou artigos técnicos).",
            params = jsonObjectOf(
                "url" to jsonObject(
                    "type" to "string",
                    "description" to "A URL completa a ser consultada."
                )
            ),
            required = listOf("url")
        ),
        tool(
            name = "finish",
            description = "Finaliza a tarefa atual e apresenta o resumo do que foi realizado ao usuário.",
            params = jsonObjectOf()
        )
    )

    private fun tool(
        name: String,
        description: String,
        params: JsonObject,
        required: List<String> = emptyList()
    ): JsonObject = buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", name)
            put("description", description)
            putJsonObject("parameters") {
                put("type", "object")
                putJsonObject("properties") {
                    params.forEach { (k, v) -> put(k, v) }
                }
                if (required.isNotEmpty()) {
                    putJsonArray("required") {
                        required.forEach { add(it) }
                    }
                }
            }
        }
    }

    private fun jsonObjectOf(vararg pairs: Pair<String, JsonObject>): JsonObject =
        buildJsonObject { pairs.forEach { (k, v) -> put(k, v) } }

    private fun jsonObject(vararg pairs: Pair<String, String>): JsonObject =
        buildJsonObject { pairs.forEach { (k, v) -> put(k, v) } }
}
