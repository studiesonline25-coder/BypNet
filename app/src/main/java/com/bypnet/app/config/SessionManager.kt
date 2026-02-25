package com.bypnet.app.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global session manager holding temporary state for the current 
 * configuration across screens, such as from the Payload Editor.
 * This lets the PayloadEditorScreen and HomeScreen share data easily
 * without complex ViewModel passing.
 */
object SessionManager {
    var payload by mutableStateOf("GET / HTTP/1.1[crlf]Host: [sni][crlf]Connection: Keep-Alive[crlf][crlf]")
    var proxyHost by mutableStateOf("")
    var proxyPort by mutableStateOf("8080")
}
