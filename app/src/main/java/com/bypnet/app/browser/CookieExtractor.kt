package com.bypnet.app.browser

import android.webkit.CookieManager
import com.bypnet.app.data.entity.CookieEntity

/**
 * Extracts cookies from Android's WebView CookieManager
 * and formats them for payload injection.
 */
object CookieExtractor {

    /**
     * Extract all cookies for a given URL from the WebView CookieManager.
     */
    fun extractCookies(url: String): List<CookieEntity> {
        val cookieString = CookieManager.getInstance().getCookie(url) ?: return emptyList()
        val domain = extractDomain(url)

        return cookieString.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .map { cookie ->
                val parts = cookie.split("=", limit = 2)
                CookieEntity(
                    domain = domain,
                    name = parts[0].trim(),
                    value = if (parts.size > 1) parts[1].trim() else "",
                    extractedAt = System.currentTimeMillis()
                )
            }
    }

    /**
     * Format cookies as a single string for HTTP header injection.
     * Output: "name1=value1; name2=value2; name3=value3"
     */
    fun formatForPayload(cookies: List<CookieEntity>): String {
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    /**
     * Format cookies for the raw cookie string from CookieManager.
     */
    fun formatRawCookie(url: String): String {
        return CookieManager.getInstance().getCookie(url) ?: ""
    }

    /**
     * Extract domain from URL.
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Clear all cookies from the WebView.
     */
    fun clearAllCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
