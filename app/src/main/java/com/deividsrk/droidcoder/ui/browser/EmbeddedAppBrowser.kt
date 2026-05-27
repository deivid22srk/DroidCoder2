package com.deividsrk.droidcoder.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deividsrk.droidcoder.browser.BrowserManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun EmbeddedAppBrowser() {
    val browserUrl by BrowserManager.url.collectAsStateWithLifecycle()
    val navigationTrigger by BrowserManager.navigationTrigger.collectAsStateWithLifecycle()
    val jsCommandQueue by BrowserManager.jsCommandQueue.collectAsStateWithLifecycle()

    var inputUrl by remember(browserUrl) { mutableStateOf(browserUrl) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Initialize WebView
    val webView = remember {
        android.webkit.WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null) {
                        // Extract plain text & HTML contents, send back to BrowserManager
                        view?.evaluateJavascript(
                            "(function() { return JSON.stringify({ html: document.documentElement.outerHTML, text: document.body.innerText }); })()"
                        ) { resultJson ->
                            try {
                                if (resultJson != null && resultJson != "null") {
                                    val cleanJson = resultJson.trim('"').replace("\\\"", "\"").replace("\\\\", "\\")
                                    val parser = Json { ignoreUnknownKeys = true }
                                    val obj = parser.parseToJsonElement(cleanJson).jsonObject
                                    val html = obj["html"]?.jsonPrimitive?.content ?: ""
                                    val text = obj["text"]?.jsonPrimitive?.content ?: ""
                                    BrowserManager.updatePageContent(html, text)
                                }
                            } catch (e: Exception) {
                                // Fallback: just extract document body innerText
                                view?.evaluateJavascript("document.body.innerText") { textResult ->
                                    val cleanText = textResult?.trim('"')?.replace("\\n", "\n") ?: ""
                                    BrowserManager.updatePageContent("", cleanText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // React to navigation triggers from AI
    LaunchedEffect(navigationTrigger) {
        navigationTrigger?.let { url ->
            webView.loadUrl(url)
            BrowserManager.clearNavigationTrigger()
        }
    }

    // React to JS commands from AI
    LaunchedEffect(jsCommandQueue) {
        jsCommandQueue?.let { js ->
            webView.evaluateJavascript(js) { result ->
                // Update contents after executing JS command
                webView.evaluateJavascript("document.body.innerText") { textResult ->
                    val cleanText = textResult?.trim('"')?.replace("\\n", "\n") ?: ""
                    BrowserManager.updatePageContent("", cleanText)
                }
            }
            BrowserManager.clearJsCommand()
        }
    }

    // Load initial page if empty
    LaunchedEffect(Unit) {
        if (webView.url == null) {
            webView.loadUrl(browserUrl)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Browser Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth().border(width = (0.5).dp, color = Color(0xFF282A36)),
            color = Color(0xFF0C0D12)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mini Window controls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
                }
                Spacer(Modifier.width(4.dp))

                // Navigation buttons
                IconButton(
                    onClick = { if (webView.canGoBack()) webView.goBack() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF6272A4), modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { webView.reload() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = Color(0xFF6272A4), modifier = Modifier.size(16.dp))
                }

                // Address Bar
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    modifier = Modifier.weight(1f).height(34.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFF8F8F2)),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF000000),
                        unfocusedContainerColor = Color(0xFF000000),
                        focusedBorderColor = Color(0xFFFF79C6),
                        unfocusedBorderColor = Color(0xFF282A36),
                        cursorColor = Color(0xFFFF79C6)
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            BrowserManager.navigate(inputUrl)
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Ir", tint = Color(0xFFA6E22E), modifier = Modifier.size(12.dp))
                        }
                    }
                )
            }
        }

        // WebView view interop
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
