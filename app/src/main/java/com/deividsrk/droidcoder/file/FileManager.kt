package com.deividsrk.droidcoder.file

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages all file system operations within the user-selected project directory.
 *
 * Uses either a standard java.io.File path (from SAF persistent permissions)
 * or direct File access depending on the Android version and setup.
 */
class FileManager(private val context: Context) {

    var projectRoot: File? = null
        private set

    val isReady: Boolean get() = projectRoot != null && projectRoot!!.exists()

    /**
     * Set the project root from a SAF tree URI.
     * On Android 10+, we use the URI-based approach with content resolver.
     */
    fun setProjectRoot(uri: Uri) {
        // Store the URI for persistent access
        val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        // Extract a usable file path from the URI
        val path = getPathFromUri(uri)
        if (path != null) {
            projectRoot = File(path)
        }
    }

    /**
     * Set project root directly from a file path.
     */
    fun setProjectRoot(path: String) {
        projectRoot = File(path)
    }

    fun getProjectName(): String = projectRoot?.name ?: "projeto"

    /**
     * Recursively lists all files in the project, skipping .git directory.
     */
    suspend fun listFiles(): List<String> = withContext(Dispatchers.IO) {
        val root = projectRoot ?: return@withContext emptyList()
        val result = mutableListOf<String>()
        walkFiles(root, root, result)
        result.sorted()
    }

    private fun walkFiles(root: File, dir: File, result: MutableList<String>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (file.name == ".git" || file.name.startsWith(".") && file.name != ".github") continue
                walkFiles(root, file, result)
            } else {
                result.add(file.relativeTo(root).path)
            }
        }
    }

    /**
     * Reads a file's full content as a string.
     */
    suspend fun readFile(relativePath: String): String = withContext(Dispatchers.IO) {
        val root = projectRoot ?: throw IllegalStateException("Projeto não configurado.")
        val file = File(root, relativePath)
        if (!file.exists()) throw IllegalArgumentException("Arquivo \"$relativePath\" não encontrado.")
        if (!file.canRead()) throw SecurityException("Sem permissão para ler \"$relativePath\".")
        file.readText()
    }

    /**
     * Writes content to a file, creating parent directories as needed.
     */
    suspend fun writeFile(relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val root = projectRoot ?: throw IllegalStateException("Projeto não configurado.")
        val file = File(root, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * Deletes a file from the project.
     */
    suspend fun deleteFile(relativePath: String) = withContext(Dispatchers.IO) {
        val root = projectRoot ?: throw IllegalStateException("Projeto não configurado.")
        val file = File(root, relativePath)
        if (!file.exists()) throw IllegalArgumentException("Arquivo \"$relativePath\" não encontrado.")
        if (!file.delete()) throw IllegalStateException("Falha ao excluir \"$relativePath\".")
    }

    /**
     * Checks if a file exists.
     */
    suspend fun fileExists(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val root = projectRoot ?: return@withContext false
        File(root, relativePath).exists()
    }

    private fun getPathFromUri(uri: Uri): String? {
        // For content:// URIs from SAF, we try to extract the path
        val docId = try {
            android.provider.DocumentsContract.getTreeDocumentId(uri)
        } catch (e: Exception) {
            null
        }
        if (docId != null) {
            val parts = docId.split(":")
            return if (parts.size >= 2) {
                val type = parts[0]
                val relativePath = parts[1]
                when (type) {
                    "primary" -> "/storage/emulated/0/$relativePath"
                    else -> "/storage/$type/$relativePath"
                }
            } else if (parts.size == 1 && parts[0] == "primary") {
                "/storage/emulated/0"
            } else {
                null
            }
        }
        return uri.path
    }
}
