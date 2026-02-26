package com.bypnet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    val tabs = listOf("HOME", "LOG")
    val context = androidx.compose.ui.platform.LocalContext.current
    val configManager = remember { ConfigManager(context) }
    var importResult by remember { mutableStateOf<String?>(null) }

    // File picker for Import
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

    // Export lock dialog state
    var showExportLockDialog by remember { mutableStateOf(false) }
    var exportLockPassword by remember { mutableStateOf("") }
    var exportLockEnabled by remember { mutableStateOf(false) }

    // File picker for Export (.byp binary format)
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

    // Lock password dialog before exporting
    if (showExportLockDialog) {
        androidx.compose.material3.AlertDialog(
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
                            colors = CheckboxDefaults.colors(checkedColor = Cyan400)
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
                                focusedBorderColor = Cyan400,
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
                    Text("Export", color = Cyan400, fontWeight = FontWeight.Bold)
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
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(DarkCard)
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Cyan400.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("BypNet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("v1.0", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Menu Items
                DrawerMenuItem(Icons.Filled.Edit, "Payload & Proxy") {
                    scope.launch { drawerState.close() }
                    onNavigate("payload_editor")
                }
                DrawerMenuItem(Icons.Filled.Search, "IP Hunter") {
                    scope.launch { drawerState.close() }
                    onNavigate("ip_hunter")
                }
                DrawerMenuItem(Icons.Filled.Wifi, "Response Checker") {
                    scope.launch { drawerState.close() }
                    onNavigate("response_checker")
                }
                DrawerMenuItem(Icons.Filled.Public, "Cookie Browser") {
                    scope.launch { drawerState.close() }
                    onNavigate("browser")
                }

                HorizontalDivider(
                    color = DarkBorder,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                DrawerMenuItem(Icons.Filled.Settings, "Settings") {
                    scope.launch { drawerState.close() }
                    onNavigate("settings")
                }
                DrawerMenuItem(Icons.Filled.Info, "About") {
                    scope.launch { drawerState.close() }
                }

                Spacer(modifier = Modifier.weight(1f))

                DrawerMenuItem(Icons.Filled.Close, "Exit", tint = StatusDisconnected) {
                    // TODO: exit
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text("BypNet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = TextPrimary)
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

            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = DarkSurface,
                contentColor = Cyan400,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Cyan400
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
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) Cyan400 else TextSecondary
                            )
                        }
                    )
                }
            }

            // Pager
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> HomeScreen()
                    1 -> LogScreen()
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color = TextSecondary,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = tint,
            unselectedTextColor = if (tint == StatusDisconnected) tint else TextPrimary
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
