package com.bypnet.app.browser

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages passing an active WebView from the background JsChallengeSolver 
 * to the foreground UI when a CAPTCHA requires manual human verification.
 */
object CaptchaDialogManager {
    
    private val _webViewFlow = MutableStateFlow<WebView?>(null)
    
    /**
     * Flow that the main UI collects to know when to show a WebView overlay.
     */
    val webViewFlow: StateFlow<WebView?> = _webViewFlow

    /**
     * Shows the specified WebView in the foreground dialog.
     */
    fun showCaptcha(webView: WebView) {
        _webViewFlow.value = webView
    }

    /**
     * Closes the foreground dialog.
     */
    fun closeCaptcha() {
        _webViewFlow.value = null
    }
}
