package com.deividsrk.droidcoder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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

        checkStoragePermissions()

        setContent {
            DroidCoder2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${packageName}")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            // Android 10 and below: request standard storage permissions
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val requestList = permissions.filter {
                checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (requestList.isNotEmpty()) {
                requestPermissions(requestList.toTypedArray(), 1001)
            }
        }
    }
}
