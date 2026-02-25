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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bypnet.app.ui.screens.BrowserScreen
import com.bypnet.app.ui.screens.HomeScreen
import com.bypnet.app.ui.screens.LogScreen
import com.bypnet.app.ui.screens.SettingsScreen
import com.bypnet.app.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BypNetTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main_pager") {
                    composable("main_pager") {
                        BypNetAppScaffold(
                            onNavigateToBrowser = { navController.navigate("browser") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("browser") {
                        BrowserScreen()
                    }
                    composable("settings") {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BypNetAppScaffold(
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val tabs = listOf("HOME", "LOG")

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
                        .height(160.dp)
                        .background(DarkCard)
                        .padding(24.dp),
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
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = Cyan400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "BypNet HTTP Custom",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v1.0-RC1",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drawer Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                    label = { Text("Clear Data") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("About Us") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
                    label = { Text("Exit") },
                    selected = false,
                    onClick = { /* Exit App */ },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = StatusDisconnected,
                        unselectedTextColor = StatusDisconnected
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            // App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "HTTP Custom",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToBrowser) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = "Browser",
                            tint = Cyan400
                        )
                    }
                    IconButton(onClick = { /* TODO: 3-dot menu */ }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
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
                divider = {
                    HorizontalDivider(color = DarkBorder)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
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

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> HomeScreen()
                    1 -> LogScreen()
                }
            }
        }
    }
}
