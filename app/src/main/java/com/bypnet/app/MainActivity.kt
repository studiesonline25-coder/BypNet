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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
                    composable("payload_editor") { PayloadEditorScreen() }
                    composable("ip_hunter") { IpHunterScreen() }
                    composable("response_checker") { ResponseCheckerScreen() }
                    composable("ssh_settings") { SshSettingsScreen() }
                    composable("vpn_settings") { VpnSettingsScreen() }
                    composable("sni") { SniScreen() }
                    composable("dns_custom") { DnsCustomScreen() }
                    composable("udpgw") { UdpgwScreen() }
                    composable("short_url") { ShortUrlScreen() }
                    composable("bnid") { BnidScreen() }
                    composable("battery_opt") { BatteryOptScreen() }
                    composable("about") { AboutScreen() }
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

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val config = configManager.importConfig(it)
                if (config != null) {
                    com.bypnet.app.config.SessionManager.loadConfig(config)
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

    // Export Lock Dialog
    if (showExportLockDialog) {
        AlertDialog(
            onDismissRequest = { showExportLockDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Export .byp Config", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exportLockEnabled,
                            onCheckedChange = { exportLockEnabled = it },
                            colors = CheckboxDefaults.colors(checkedColor = StatusConnected)
                        )
                        Text("Lock with password", color = TextPrimary, fontSize = 14.sp)
                    }
                    if (exportLockEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = exportLockPassword,
                            onValueChange = { exportLockPassword = it },
                            label = { Text("Password", color = TextTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StatusConnected,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportLockDialog = false
                    exportLauncher.launch("config.byp")
                }) {
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(280.dp)
            ) {
                // ── Drawer Header (HC logo area) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(DarkCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        // HC-style hexagon logo
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StatusConnected.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", color = StatusConnected, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("2026 © BypNet Dev.", color = TextPrimary, fontSize = 13.sp)
                        Text("v1.0.0", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Simple Maker ──
                DrawerMenuItem(Icons.Filled.AutoAwesome, "Simple Maker") {
                    scope.launch { drawerState.close() }
                    onNavigate("payload_editor")
                }

                // ── Utility ──
                DrawerSectionHeader("Utility")
                DrawerMenuItem(Icons.Filled.GridView, "Payload") {
                    scope.launch { drawerState.close() }
                    onNavigate("payload_editor")
                }
                DrawerMenuItem(Icons.Filled.Verified, "SNI") {
                    scope.launch { drawerState.close() }
                    onNavigate("sni")
                }

                // ── Connection ──
                DrawerSectionHeader("Connection")
                DrawerMenuItem(Icons.Filled.Sync, "SSH Settings") {
                    scope.launch { drawerState.close() }
                    onNavigate("ssh_settings")
                }
                DrawerMenuItem(Icons.Filled.VpnKey, "VPN Settings") {
                    scope.launch { drawerState.close() }
                    onNavigate("vpn_settings")
                }

                // ── Tool ──
                DrawerSectionHeader("Tool")
                DrawerMenuItem(Icons.Filled.Dns, "DNS Custom") {
                    scope.launch { drawerState.close() }
                    onNavigate("dns_custom")
                }
                DrawerMenuItem(Icons.Filled.Cable, "UDPGW SSH") {
                    scope.launch { drawerState.close() }
                    onNavigate("udpgw")
                }
                DrawerMenuItem(Icons.Filled.Code, "Response Checker") {
                    scope.launch { drawerState.close() }
                    onNavigate("response_checker")
                }
                DrawerMenuItem(Icons.Filled.Search, "Mobile IP Hunter") {
                    scope.launch { drawerState.close() }
                    onNavigate("ip_hunter")
                }
                DrawerMenuItem(Icons.Filled.Link, "ShortUrl Maker") {
                    scope.launch { drawerState.close() }
                    onNavigate("short_url")
                }
                DrawerMenuItem(Icons.Filled.Public, "Cookie Browser") {
                    scope.launch { drawerState.close() }
                    onNavigate("browser")
                }
                DrawerMenuItem(Icons.Filled.Fingerprint, "BNID") {
                    scope.launch { drawerState.close() }
                    onNavigate("bnid")
                }
                DrawerMenuItem(Icons.Filled.BatteryChargingFull, "Battery Optimization") {
                    scope.launch { drawerState.close() }
                    onNavigate("battery_opt")
                }
                DrawerMenuItem(Icons.Filled.Info, "About") {
                    scope.launch { drawerState.close() }
                    onNavigate("about")
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBackground,
            // ── Green FAB (bottom right, like HTTP Custom's + button) ──
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    // Expanded FAB options
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
                                fabExpanded = false
                                showExportLockDialog = true
                            }
                            FabOption("Open Config", Icons.Filled.FolderOpen) {
                                fabExpanded = false
                                importLauncher.launch(arrayOf("*/*"))
                            }
                            FabOption("Cloud Config", Icons.Filled.CloudDownload) {
                                fabExpanded = false
                                // Cloud config placeholder
                                Toast.makeText(context, "Cloud config coming soon", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = StatusConnected,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                // ── Top App Bar (HC style) ──
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Byp",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Text(
                                "Net",
                                color = StatusConnected,
                                fontWeight = FontWeight.Light,
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Favorite/star */ }) {
                            Icon(Icons.Filled.Star, null, tint = TextPrimary)
                        }
                        IconButton(onClick = { /* Cloud download */ }) {
                            Icon(Icons.Filled.CloudDownload, null, tint = StatusConnected)
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, null, tint = TextPrimary)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import Config (.byp)", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.FolderOpen, null, tint = TextSecondary) },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Config (.byp)", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Filled.Save, null, tint = TextSecondary) },
                                    onClick = {
                                        showMenu = false
                                        showExportLockDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )

                // ── Tab Row: SSH | LOG ──
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = DarkSurface,
                    contentColor = StatusConnected,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = StatusConnected
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = DarkBorder) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pagerState.currentPage == index) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }

                // ── Pager: Home | Log ──
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> HomeScreen()
                        1 -> LogScreen()
                    }
                }
            }
        }
    }
}

// ── Drawer Components ──

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        title,
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = TextSecondary,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        label = { Text(label, fontSize = 14.sp) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = tint,
            unselectedTextColor = TextPrimary
        ),
        modifier = Modifier.padding(horizontal = 8.dp).height(44.dp)
    )
}

// ── FAB Option ──

@Composable
fun FabOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = DarkSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = StatusConnected,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
    }
}
