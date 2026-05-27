package com.deividsrk.droidcoder.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BrowserManager {
    // Current URL of the browser
    private val _url = MutableStateFlow("https://google.com")
    val url: StateFlow<String> = _url.asStateFlow()

    // Flag indicating the AI requested a navigation
    private val _navigationTrigger = MutableStateFlow<String?>(null)
    val navigationTrigger: StateFlow<String?> = _navigationTrigger.asStateFlow()

    // Command queue to send Javascript commands (like clicking, typing) to the WebView
    private val _jsCommandQueue = MutableStateFlow<String?>(null)
    val jsCommandQueue: StateFlow<String?> = _jsCommandQueue.asStateFlow()

    // The current page content (HTML or clean text) reported back from WebView
    private var _currentHtml = ""
    private var _currentText = ""

    fun navigate(newUrl: String) {
        var formattedUrl = newUrl
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        _url.value = formattedUrl
        _navigationTrigger.value = formattedUrl
    }

    fun clearNavigationTrigger() {
        _navigationTrigger.value = null
    }

    fun executeJs(js: String) {
        _jsCommandQueue.value = js
    }

    fun clearJsCommand() {
        _jsCommandQueue.value = null
    }

    fun updatePageContent(html: String, text: String) {
        _currentHtml = html
        _currentText = text
    }

    fun getCurrentHtml(): String = _currentHtml
    fun getCurrentText(): String = _currentText
}
