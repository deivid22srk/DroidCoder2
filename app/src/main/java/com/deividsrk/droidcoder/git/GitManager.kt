package com.deividsrk.droidcoder.git

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * Manages all Git operations: clone, status, commit, push.
 * Uses JGit for pure-Java Git implementation — no native git binary needed.
 */
class GitManager(
    private val context: Context,
    private val getToken: () -> String,
    private val getRepoUrl: () -> String,
    private val getAuthorName: () -> String,
    private val getAuthorEmail: () -> String
) {

    private var gitDir: File? = null

    fun setProjectRoot(root: File) {
        gitDir = root
    }

    val isRepo: Boolean
        get() {
            val dir = gitDir ?: return false
            return File(dir, ".git").exists()
        }

    /**
     * Initialize a new Git repository in the project directory.
     */
    suspend fun init(): String = withContext(Dispatchers.IO) {
        val dir = gitDir ?: throw IllegalStateException("Diretório do projeto não configurado.")
        if (isRepo) return@withContext "Repositório Git já existe."
        Git.init().setDirectory(dir).call().use { git ->
            // Configure user
            git.repository.config.apply {
                setString("user", null, "name", getAuthorName())
                setString("user", null, "email", getAuthorEmail())
                save()
            }
        }
        "Repositório Git inicializado."
    }

    /**
     * Clone a remote repository into the project directory.
     */
    suspend fun clone(url: String, branch: String = "main"): String = withContext(Dispatchers.IO) {
        val dir = gitDir ?: throw IllegalStateException("Diretório do projeto não configurado.")

        // Clear directory if not empty
        if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) {
            dir.deleteRecursively()
        }
        dir.mkdirs()

        val git = Git.cloneRepository()
            .setURI(url)
            .setDirectory(dir)
            .setBranch(branch)
            .setCredentialsProvider(UsernamePasswordCredentialsProvider(getToken(), ""))
            .setCloneAllBranches(false)
            .setDepth(1)
            .call()

        git.use {
            it.repository.config.apply {
                setString("user", null, "name", getAuthorName())
                setString("user", null, "email", getAuthorEmail())
                save()
            }
        }

        "Repositório clonado com sucesso: $url"
    }

    /**
     * Get the current Git status.
     */
    suspend fun getStatus(): String = withContext(Dispatchers.IO) {
        val dir = gitDir ?: throw IllegalStateException("Diretório não configurado.")
        if (!isRepo) return@withContext "Não é um repositório Git."

        Git.open(dir).use { git ->
            val status = git.status().call()
            val sb = StringBuilder()

            if (status.added.isNotEmpty()) {
                sb.appendLine("➕ Adicionados:")
                status.added.forEach { sb.appendLine("   $it") }
            }
            if (status.changed.isNotEmpty()) {
                sb.appendLine("✏️ Modificados:")
                status.changed.forEach { sb.appendLine("   $it") }
            }
            if (status.removed.isNotEmpty()) {
                sb.appendLine("🗑️ Removidos:")
                status.removed.forEach { sb.appendLine("   $it") }
            }
            if (status.untracked.isNotEmpty()) {
                sb.appendLine("❓ Não rastreados:")
                status.untracked.forEach { sb.appendLine("   $it") }
            }
            if (sb.isEmpty()) sb.appendLine("✅ Nada para commitar (working tree limpa).")
            sb.toString().trim()
        }
    }

    /**
     * Stage all changes and commit.
     */
    suspend fun commit(message: String): String = withContext(Dispatchers.IO) {
        val dir = gitDir ?: throw IllegalStateException("Diretório não configurado.")

        Git.open(dir).use { git ->
            // Stage all (git add .)
            git.add().addFilepattern(".").call()

            // Commit
            val commit = git.commit()
                .setMessage(message)
                .setAuthor(getAuthorName(), getAuthorEmail())
                .call()

            commit.name  // SHA
        }
    }

    /**
     * Push commits to the configured remote repository.
     */
    suspend fun push(): String = withContext(Dispatchers.IO) {
        val dir = gitDir ?: throw IllegalStateException("Diretório não configurado.")
        val repoUrl = getRepoUrl()
        if (repoUrl.isBlank()) throw IllegalStateException("URL do repositório remoto não configurada.")

        Git.open(dir).use { git ->
            // Ensure remote origin exists
            val config = git.repository.config
            val remotes = git.remoteList().call()
            val hasOrigin = remotes.any { it.name == "origin" }

            if (!hasOrigin) {
                git.remoteAdd()
                    .setName("origin")
                    .setUri(org.eclipse.jgit.transport.URIish(repoUrl))
                    .call()
            }

            val push = git.push()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(getToken(), ""))
                .setForce(true)
                .setPushAll()

            push.call().forEach { result ->
                result.messages?.let { msg ->
                    if (msg.isNotBlank()) println("Git push: $msg")
                }
            }
        }

        "Push concluído com sucesso!"
    }
}
