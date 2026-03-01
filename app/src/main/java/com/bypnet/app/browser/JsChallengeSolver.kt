package com.bypnet.app.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class JsChallengeResult(
    val isSuccess: Boolean,
    val cookies: String,
    val userAgent: String,
    val errorMessage: String? = null
)

/**
 * Headless JS Challenge Auto-Solver using a background/invisible WebView.
 * It loads a given URL, waits for Cloudflare/DDoS-Guard clearances,
 * and extracts the valid cookies and User-Agent.
 */
class JsChallengeSolver(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun solveChallenge(url: String, timeoutMs: Long = 15000L): JsChallengeResult {
        return withContext(Dispatchers.Main) {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val webView = WebView(context)
                    
                    // Setup stealth/headless settings
                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        // Disguise WebView
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    // Clear previous cookies to ensure fresh challenge
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().setAcceptCookie(true)

                    var isFinished = false
                    var maxTries = 40 // Initial 20s for auto-solve
                    var currentTry = 0
                    val handler = Handler(Looper.getMainLooper())

                    val checkRunnable = object : Runnable {
                        override fun run() {
                            if (isFinished) return
                            currentTry++

                            val cookies = CookieManager.getInstance().getCookie(url) ?: ""
                            
                            // Check for common clearance cookies (Cloudflare, DDoS-Guard, Sucuri, Imperva/Incapsula)
                            if (cookies.contains("cf_clearance") || 
                                cookies.contains("__ddg_") || 
                                cookies.contains("sucuri_cloudproxy") ||
                                cookies.contains("visid_incap_") ||
                                cookies.contains("incap_ses_")) {
                                
                                isFinished = true
                                val result = JsChallengeResult(
                                    isSuccess = true,
                                    cookies = cookies,
                                    userAgent = webView.settings.userAgentString
                                )
                                CaptchaDialogManager.closeCaptcha()
                                webView.destroy()
                                if (continuation.isActive) continuation.resume(result)
                                return
                            }

                            if (currentTry == 10 && !isFinished) { // After 5s, assume manual CAPTCHA needed
                                maxTries = 180 // Expand timeout to 90s for human solving
                                CaptchaDialogManager.showCaptcha(webView)
                            }

                            if (currentTry >= maxTries) {
                                isFinished = true
                                val result = JsChallengeResult(
                                    isSuccess = false,
                                    cookies = cookies, // return whatever we got
                                    userAgent = webView.settings.userAgentString,
                                    errorMessage = "Timeout waiting for JS clearance cookie."
                                )
                                CaptchaDialogManager.closeCaptcha()
                                webView.destroy()
                                if (continuation.isActive) continuation.resume(result)
                                return
                            }

                            // Wait 500ms and check again
                            handler.postDelayed(this, 500)
                        }
                    }

                    val autoClickerScript = """
                        (function() {
                            setInterval(function() {
                                // Cloudflare Turnstile generic checkbox
                                var cfCheckbox = document.querySelector('input[type="checkbox"]');
                                if (cfCheckbox && !cfCheckbox.checked) {
                                    cfCheckbox.click();
                                }
                                
                                // Alternative Turnstile wrapper click
                                var turnstileWrapper = document.querySelector('.ctp-checkbox-container');
                                if (turnstileWrapper) {
                                    turnstileWrapper.click();
                                }
                            }, 1000);
                        })();
                    """.trimIndent()
                    
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Start polling for cookies once the page finishes its initial load
                            handler.post(checkRunnable)
                            
                            // Inject auto-clicker script for Cloudflare Turnstile
                            view?.evaluateJavascript(autoClickerScript, null)
                        }
                    }
                    
                    webView.webChromeClient = WebChromeClient()

                    continuation.invokeOnCancellation {
                        isFinished = true
                        handler.removeCallbacksAndMessages(null)
                        CaptchaDialogManager.closeCaptcha()
                        webView.destroy()
                    }

                    // Start the process
                    webView.loadUrl(url)
                }
            } ?: JsChallengeResult(
                isSuccess = false,
                cookies = "",
                userAgent = "",
                errorMessage = "Solver timed out completely after ${timeoutMs}ms"
            )
        }
    }
}
