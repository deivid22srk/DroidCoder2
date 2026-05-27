package com.deividsrk.droidcoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.deividsrk.droidcoder.ui.theme.DroidCoder2Theme
import com.deividsrk.droidcoder.ui.MainScreen

/**
 * Main entry point for DroidCoder2.
 *
 * Material You 3 themed AI coding assistant for Android.
 * Features:
 * - AI-powered code generation and editing via OpenAI-compatible API
 * - Filesystem operations (read, write, delete files)
 * - Git operations (clone, commit, push) via JGit
 * - Folder selection via SAF
 * - Native C/C++ module for heavy processing
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DroidCoder2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}
