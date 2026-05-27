package com.deividsrk.droidcoder.file

/**
 * JNI bridge to the native C++ module (droidcoder_native).
 *
 * Heavy operations are delegated here for performance:
 * - SHA-256 hashing for file integrity
 * - Fast text search (Boyer-Moore-Horspool)
 * - Line/word/character counting
 * - Diff computation
 */
object NativeBridge {

    init {
        try {
            System.loadLibrary("droidcoder_native")
        } catch (e: UnsatisfiedLinkError) {
            // Native library not available — fall back to Kotlin implementations
            android.util.Log.w("DroidCoder2", "Native library not loaded: ${e.message}")
        }
    }

    val isNativeAvailable: Boolean by lazy {
        try {
            sha256Hash("test")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Compute SHA-256 hash of a string.
     */
    external fun sha256Hash(input: String): String

    /**
     * Compute a line-level diff between two texts.
     */
    external fun computeDiff(oldText: String, newText: String): String

    /**
     * Fast text search. Returns byte offset or -1.
     */
    external fun fastSearch(haystack: String, needle: String): Int

    /**
     * Count lines, words, and characters. Returns "lines:words:chars".
     */
    external fun countStats(text: String): String

    // ---- Kotlin fallback implementations ----

    fun sha256HashFallback(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun countStatsFallback(text: String): String {
        val lines = text.lines().size
        val words = text.split(Regex("\\s+")).count { it.isNotEmpty() }
        val chars = text.length
        return "$lines:$words:$chars"
    }

    fun sha256(input: String): String = if (isNativeAvailable) sha256Hash(input) else sha256HashFallback(input)

    fun stats(text: String): String = if (isNativeAvailable) countStats(text) else countStatsFallback(text)
}
