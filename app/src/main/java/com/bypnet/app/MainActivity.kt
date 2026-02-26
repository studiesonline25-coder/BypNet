package com.bypnet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bypnet.app.ui.screens.*
import com.bypnet.app.ui.theme.*
import com.bypnet.app.config.ConfigManager
import com.bypnet.app.config.BypConfig
import com.bypnet.app.config.BypConfigSerializer
import com.bypnet.app.config.SessionManager
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BypNetTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        BypNetMainScaffold(
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable("browser") { BrowserScreen() }
                    composable("settings") { SettingsScreen() }
                    composable("ip_hunter") { IpHunterScreen() }
                    composable("response_checker") { ResponseCheckerScreen() }
                    composable("ssh_settings") { SshSettingsScreen() }
                    composable("vpn_settings") { VpnSettingsScreen() }
                    composable("dns_custom") { DnsCustomScreen() }
                    composable("udpgw") { UdpgwScreen() }
                    composable("short_url") { ShortUrlScreen() }
                    composable("bnid") { BnidScreen() }
                    composable("battery_opt") { BatteryOptScreen() }
                    composable("about") { AboutScreen() }
                    composable("simple_maker") {
                        SimpleMakerScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BypNetMainScaffold(onNavigate: (String) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabs = listOf("SSH", "LOG")
    val context = androidx.compose.ui.platform.LocalContext.current
    val configManager = remember { ConfigManager(context) }

    // FAB expanded state
    var fabExpanded by remember { mutableStateOf(false) }

    // Payload dialog state
    var showPayloadDialog by remember { mutableStateOf(false) }
    var payloadDialogText by remember { mutableStateOf(SessionManager.payload) }
    var payloadDialogProxy by remember { mutableStateOf(
        if (SessionManager.proxyHost.isNotEmpty())
            "${SessionManager.proxyHost}:${SessionManager.proxyPort}"
        else ""
    ) }

    // SNI dialog state
    var showSniDialog by remember { mutableStateOf(false) }
    var sniDialogText by remember { mutableStateOf(SessionManager.sni) }

    // Destination Ping dialog state
    var showPingDialog by remember { mutableStateOf(false) }
    var pingHost by remember { mutableStateOf("") }
    var pingResult by remember { mutableStateOf("") }
    var pinging by remember { mutableStateOf(false) }

    // Timeout dialog state
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var timeoutValue by remember { mutableStateOf("30") }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val config = configManager.importConfig(it)
                if (config != null) {
                    SessionManager.loadConfig(config)
                    Toast.makeText(context, "✓ Imported: ${config.name.ifEmpty { "Config" }}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "✗ Failed to import config", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Export dialog state
    var showExportLockDialog by remember { mutableStateOf(false) }
    var exportLockPassword by remember { mutableStateOf("") }
    var exportLockEnabled by remember { mutableStateOf(false) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val config = BypConfig(name = "BypNet Export")
                    val bypBytes = if (exportLockEnabled && exportLockPassword.isNotEmpty()) {
                        BypConfigSerializer.toLockedByp(config, exportLockPassword)
                    } else {
                        BypConfigSerializer.toByp(config)
                    }
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(bypBytes)
                    }
                    val lockMsg = if (exportLockEnabled) " (locked 🔒)" else ""
                    Toast.makeText(context, "✓ Config exported$lockMsg", Toast.LENGTH_SHORT).show()
                    exportLockPassword = ""
                    exportLockEnabled = false
                } catch (e: Exception) {
                    Toast.makeText(context, "✗ Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── DIALOGS ──

    // Export Lock Dialog
    if (showExportLockDialog) {
        AlertDialog(
            onDismissRequest = { showExportLockDialog = false },
            containerColor = DarkSurface,
            title = { Text("Export .byp Config", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = exportLockEnabled, onCheckedChange = { exportLockEnabled = it },
                            colors = CheckboxDefaults.colors(checkedColor = StatusConnected))
                        Text("Lock with password", color = TextPrimary, fontSize = 14.sp)
                    }
                    if (exportLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = exportLockPassword, onValueChange = { exportLockPassword = it },
                            label = { Text("Password", color = TextTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportLockDialog = false; exportLauncher.launch("config.byp") }) {
                    Text("Export", color = StatusConnected, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportLockDialog = false }) {
                    Text("Cancel", color = TextTertiary)
                }
            }
        )
    }

    // Payload Dialog (HC-style: Payload field + Remote Proxy + Apply)
    if (showPayloadDialog) {
        AlertDialog(
            onDismissRequest = { showPayloadDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payload", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showPayloadDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextSecondary)
                    }
                }
            },
            text = {
                Column {
                    HorizontalDivider(color = StatusConnected, thickness = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = payloadDialogText,
                        onValueChange = { payloadDialogText = it },
                        placeholder = { Text("Payload", color = TextTertiary, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            cursorColor = StatusConnected),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = payloadDialogProxy,
                        onValueChange = { payloadDialogProxy = it },
                        placeholder = { Text("Remote Proxy (ip:port@user:pass)", color = TextTertiary, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            cursorColor = StatusConnected),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SessionManager.payload = payloadDialogText
                        if (payloadDialogProxy.isNotEmpty()) {
                            val parts = payloadDialogProxy.split(":", limit = 2)
                            SessionManager.proxyHost = parts.getOrElse(0) { "" }
                            SessionManager.proxyPort = parts.getOrElse(1) { "8080" }
                        }
                        showPayloadDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {}
        )
    }

    // SNI Dialog (HC-style: SNI field + Apply)
    if (showSniDialog) {
        AlertDialog(
            onDismissRequest = { showSniDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SNI", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showSniDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextSecondary)
                    }
                }
            },
            text = {
                Column {
                    HorizontalDivider(color = StatusConnected, thickness = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = sniDialogText,
                        onValueChange = { sniDialogText = it },
                        placeholder = { Text("Server Name Indication", color = TextTertiary, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            cursorColor = StatusConnected),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { SessionManager.sni = sniDialogText; showSniDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            dismissButton = {}
        )
    }

    // Destination Ping Dialog
    if (showPingDialog) {
        AlertDialog(
            onDismissRequest = { showPingDialog = false },
            containerColor = DarkSurface,
            title = { Text("Destination Ping", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pingHost, onValueChange = { pingHost = it },
                        placeholder = { Text("Host to ping", color = TextTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp)
                    )
                    if (pingResult.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(pingResult, color = StatusConnected, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pinging = true
                        scope.launch {
                            pingResult = try {
                                val start = System.currentTimeMillis()
                                val addr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    InetAddress.getByName(pingHost)
                                }
                                val reachable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    addr.isReachable(5000)
                                }
                                val elapsed = System.currentTimeMillis() - start
                                if (reachable) "✓ ${addr.hostAddress} — ${elapsed}ms" else "✗ Unreachable"
                            } catch (e: Exception) { "✗ ${e.message}" }
                            pinging = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                    shape = RoundedCornerShape(6.dp), enabled = !pinging
                ) { Text("Ping", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPingDialog = false }) { Text("Close", color = TextTertiary) }
            }
        )
    }

    // Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            containerColor = DarkSurface,
            title = { Text("Timeout", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = timeoutValue, onValueChange = { timeoutValue = it },
                    placeholder = { Text("Seconds", color = TextTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusConnected, unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTimeoutDialog = false; Toast.makeText(context, "Timeout set to ${timeoutValue}s", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusConnected, contentColor = DarkBackground),
                    shape = RoundedCornerShape(6.dp)
                ) { Text("Apply", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel", color = TextTertiary) }
            }
        )
    }

    // ── MAIN LAYOUT ──

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = DarkSurface, modifier = Modifier.width(280.dp)) {
                // Drawer Header
                Box(
                    modifier = Modifier.fillMaxWidth().height(130.dp).background(DarkCard).padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(StatusConnected.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Text("B", color = StatusConnected, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(8.dp))
                        Text("2026 © BypNet Dev.", color = TextPrimary, fontSize = 13.sp)
                        Text("v1.0.0", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Simple Maker
                DrawerMenuItem(Icons.Filled.AutoAwesome, "Simple Maker") {
                    scope.launch { drawerState.close() }; onNavigate("simple_maker")
                }

                // Utility
                DrawerSectionHeader("Utility")
                DrawerMenuItem(Icons.Filled.GridView, "Payload") {
                    scope.launch { drawerState.close() }
                    payloadDialogText = SessionManager.payload
                    payloadDialogProxy = if (SessionManager.proxyHost.isNotEmpty())
                        "${SessionManager.proxyHost}:${SessionManager.proxyPort}" else ""
                    showPayloadDialog = true
                }
                DrawerMenuItem(Icons.Filled.Verified, "SNI") {
                    scope.launch { drawerState.close() }
                    sniDialogText = SessionManager.sni
                    showSniDialog = true
                }

                // Connection
                DrawerSectionHeader("Connection")
                DrawerMenuItem(Icons.Filled.Sync, "SSH Settings") {
                    scope.launch { drawerState.close() }; onNavigate("ssh_settings")
                }
                DrawerMenuItem(Icons.Filled.VpnKey, "VPN Settings") {
                    scope.launch { drawerState.close() }; onNavigate("vpn_settings")
                }

                // Tool
                DrawerSectionHeader("Tool")
                DrawerMenuItem(Icons.Filled.Dns, "DNS Custom") {
                    scope.launch { drawerState.close() }; onNavigate("dns_custom")
                }
                DrawerMenuItem(Icons.Filled.Cable, "UDPGW SSH") {
                    scope.launch { drawerState.close() }; onNavigate("udpgw")
                }
                DrawerMenuItem(Icons.Filled.Code, "Response Checker") {
                    scope.launch { drawerState.close() }; onNavigate("response_checker")
                }
                DrawerMenuItem(Icons.Filled.Search, "Mobile IP Hunter") {
                    scope.launch { drawerState.close() }; onNavigate("ip_hunter")
                }
                DrawerMenuItem(Icons.Filled.Link, "ShortUrl Maker") {
                    scope.launch { drawerState.close() }; onNavigate("short_url")
                }
                DrawerMenuItem(Icons.Filled.Public, "Cookie Browser") {
                    scope.launch { drawerState.close() }; onNavigate("browser")
                }
                DrawerMenuItem(Icons.Filled.Fingerprint, "BNID") {
                    scope.launch { drawerState.close() }; onNavigate("bnid")
                }
                DrawerMenuItem(Icons.Filled.BatteryChargingFull, "Battery Optimization") {
                    scope.launch { drawerState.close() }; onNavigate("battery_opt")
                }
                DrawerMenuItem(Icons.Filled.Info, "About") {
                    scope.launch { drawerState.close() }; onNavigate("about")
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            FabOption("Save Config", Icons.Filled.Save) {
                                fabExpanded = false; showExportLockDialog = true
                            }
                            FabOption("Open Config", Icons.Filled.FolderOpen) {
                                fabExpanded = false; importLauncher.launch(arrayOf("*/*"))
                            }
                            FabOption("Cloud Config", Icons.Filled.CloudDownload) {
                                fabExpanded = false
                                Toast.makeText(context, "Cloud config coming soon", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = StatusConnected, contentColor = Color.White,
                        shape = CircleShape, modifier = Modifier.size(56.dp)
                    ) {
                        Icon(if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add, null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(DarkBackground)
            ) {
                // ── Top App Bar ──
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Byp", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontStyle = FontStyle.Italic)
                            Text("Net", color = StatusConnected, fontWeight = FontWeight.Light, fontSize = 20.sp, fontStyle = FontStyle.Italic)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, "Menu", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Filled.Star, null, tint = TextPrimary) }
                        IconButton(onClick = {}) { Icon(Icons.Filled.CloudDownload, null, tint = StatusConnected) }

                        // ── 3-dot overflow menu (HC style) ──
                        var showMenu by remember { mutableStateOf(false) }
                        var showSshVpnSub by remember { mutableStateOf(false) }
                        var showUtilitySub by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true; showSshVpnSub = false; showUtilitySub = false }) {
                                Icon(Icons.Filled.MoreVert, null, tint = TextPrimary)
                            }

                            // Main overflow menu
                            DropdownMenu(
                                expanded = showMenu && !showSshVpnSub && !showUtilitySub,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                // ShareNet →
                                DropdownMenuItem(
                                    text = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("ShareNet", color = TextPrimary); Text("›", color = StatusConnected, fontSize = 16.sp)
                                    } },
                                    onClick = {
                                        showMenu = false
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Check out BypNet VPN!")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                    }
                                )
                                // SSH/VPN Settings →
                                DropdownMenuItem(
                                    text = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("SSH/VPN Settings", color = TextPrimary); Text("›", color = StatusConnected, fontSize = 16.sp)
                                    } },
                                    onClick = { showSshVpnSub = true }
                                )
                                // Utility →
                                DropdownMenuItem(
                                    text = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Utility", color = TextPrimary); Text("›", color = StatusConnected, fontSize = 16.sp)
                                    } },
                                    onClick = { showUtilitySub = true }
                                )
                                // Proxified Apps
                                DropdownMenuItem(
                                    text = { Text("Proxified Apps", color = TextPrimary) },
                                    onClick = { showMenu = false; Toast.makeText(context, "Split tunnel apps coming soon", Toast.LENGTH_SHORT).show() }
                                )
                                // Clear/Reset Config
                                DropdownMenuItem(
                                    text = { Text("Clear/Reset Config", color = TextPrimary) },
                                    onClick = { showMenu = false; SessionManager.clear(); Toast.makeText(context, "Config reset", Toast.LENGTH_SHORT).show() }
                                )
                                // Destination Ping
                                DropdownMenuItem(
                                    text = { Text("Destination Ping", color = TextPrimary) },
                                    onClick = { showMenu = false; showPingDialog = true }
                                )
                                // Timeout
                                DropdownMenuItem(
                                    text = { Text("Timeout", color = TextPrimary) },
                                    onClick = { showMenu = false; showTimeoutDialog = true }
                                )
                                // Exit
                                DropdownMenuItem(
                                    text = { Text("Exit", color = TextPrimary) },
                                    onClick = { showMenu = false; (context as? android.app.Activity)?.finishAffinity() }
                                )
                            }

                            // SSH/VPN Settings submenu
                            DropdownMenu(
                                expanded = showMenu && showSshVpnSub,
                                onDismissRequest = { showSshVpnSub = false; showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Psiphon Settings", color = TextPrimary) },
                                    onClick = { showMenu = false; showSshVpnSub = false; Toast.makeText(context, "Psiphon settings coming soon", Toast.LENGTH_SHORT).show() }
                                )
                                DropdownMenuItem(
                                    text = { Text("V2ray Settings", color = TextPrimary) },
                                    onClick = { showMenu = false; showSshVpnSub = false; onNavigate("vpn_settings") }
                                )
                                DropdownMenuItem(
                                    text = { Text("SlowDNS Settings", color = TextPrimary) },
                                    onClick = { showMenu = false; showSshVpnSub = false; onNavigate("dns_custom") }
                                )
                                DropdownMenuItem(
                                    text = { Text("UDP Tweak Settings", color = TextPrimary) },
                                    onClick = { showMenu = false; showSshVpnSub = false; onNavigate("vpn_settings") }
                                )
                            }

                            // Utility submenu
                            DropdownMenu(
                                expanded = showMenu && showUtilitySub,
                                onDismissRequest = { showUtilitySub = false; showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Payload", color = TextPrimary) },
                                    onClick = {
                                        showMenu = false; showUtilitySub = false
                                        payloadDialogText = SessionManager.payload
                                        payloadDialogProxy = if (SessionManager.proxyHost.isNotEmpty())
                                            "${SessionManager.proxyHost}:${SessionManager.proxyPort}" else ""
                                        showPayloadDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("SNI", color = TextPrimary) },
                                    onClick = {
                                        showMenu = false; showUtilitySub = false
                                        sniDialogText = SessionManager.sni
                                        showSniDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )

                // ── Tab Row ──
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = DarkSurface, contentColor = StatusConnected,
                    indicator = { TabRowDefaults.SecondaryIndicator(color = StatusConnected) },
                    divider = { HorizontalDivider(color = DarkBorder) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pagerState.currentPage == index) TextPrimary else TextSecondary)
                            }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) { 0 -> HomeScreen(); 1 -> LogScreen() }
                }
            }
        }
    }
}

// ── Drawer Components ──

@Composable
fun DrawerSectionHeader(title: String) {
    Text(title, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
fun DrawerMenuItem(icon: ImageVector, label: String, tint: Color = TextSecondary, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
        label = { Text(label, fontSize = 14.sp) },
        selected = false, onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent, unselectedIconColor = tint, unselectedTextColor = TextPrimary),
        modifier = Modifier.padding(horizontal = 8.dp).height(44.dp)
    )
}

@Composable
fun FabOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Surface(color = DarkSurface, shape = RoundedCornerShape(6.dp), shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 8.dp)) {
            Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
        }
        FloatingActionButton(onClick = onClick, containerColor = StatusConnected, contentColor = Color.White,
            shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
    }
}
