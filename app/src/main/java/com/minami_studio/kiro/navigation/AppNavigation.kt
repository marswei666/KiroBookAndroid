package com.minami_studio.kiro.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.ui.collection.CollectionScreen
import com.minami_studio.kiro.ui.entry.AddEntryScreen
import com.minami_studio.kiro.ui.entry.EntryDetailScreen
import com.minami_studio.kiro.ui.home.HomeScreen
import com.minami_studio.kiro.ui.map.FullMapScreen
import com.minami_studio.kiro.ui.map.MapTabScreen
import com.minami_studio.kiro.ui.profile.ProfileScreen
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.LanguageManager

enum class Tab(val icon: ImageVector, val labelRes: String) {
    home(Icons.Default.Home, "tab_home"),
    map(Icons.Default.Map, "tab_map"),
    collection(Icons.Default.Bookmark, "tab_collection"),
    profile(Icons.Default.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val entryStore = remember { EntryStore(context) }
    val langManager = remember { LanguageManager(context) }

    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(Tab.home) }
    var showAddEntry by remember { mutableStateOf(false) }

    val entries by entryStore.entries.collectAsState()
    val customCategories by entryStore.customCategories.collectAsState()
    val language by langManager.language.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) {
        // 主内容区域
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    entries = entries,
                    customCategories = customCategories,
                    language = language,
                    langManager = langManager,
                    entryStore = entryStore,
                    onEntryClick = { entryId ->
                        navController.navigate("entry_detail/$entryId")
                    }
                )
            }
            composable("map") {
                MapTabScreen(
                    entries = entries,
                    customCategories = customCategories,
                    language = language,
                    langManager = langManager,
                    entryStore = entryStore,
                    onEntryClick = { entryId ->
                        navController.navigate("entry_detail/$entryId")
                    }
                )
            }
            composable("collection") {
                CollectionScreen(
                    entries = entries,
                    customCategories = customCategories,
                    language = language,
                    langManager = langManager,
                    entryStore = entryStore,
                    onEntryClick = { entryId ->
                        navController.navigate("entry_detail/$entryId")
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    entries = entries,
                    customCategories = customCategories,
                    language = language,
                    entryStore = entryStore,
                    langManager = langManager
                )
            }
            composable(
                "entry_detail/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                val entry = entries.firstOrNull { it.id == entryId }
                if (entry != null) {
                    EntryDetailScreen(
                        entry = entry,
                        entryStore = entryStore,
                        language = language,
                        langManager = langManager,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("edit_entry/${entry.id}") },
                        onFullMap = { lat, lng ->
                            navController.navigate("full_map/${entry.id}")
                        }
                    )
                }
            }
            composable(
                "edit_entry/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                val editEntry = entries.firstOrNull { it.id == entryId }
                if (editEntry != null) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { navController.popBackStack() },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WanderWarm)
                        ) {
                            AddEntryScreen(
                                entryStore = entryStore,
                                langManager = langManager,
                                editEntry = editEntry,
                                onDismiss = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            composable(
                "full_map/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                val mapEntry = entries.firstOrNull { it.id == entryId }
                if (mapEntry != null && mapEntry.latitude != null && mapEntry.longitude != null) {
                    val mapIconName = entryStore.categoryIcon(mapEntry)
                    FullMapScreen(
                        latitude = mapEntry.latitude,
                        longitude = mapEntry.longitude,
                        name = mapEntry.name,
                        city = mapEntry.city,
                        country = mapEntry.country,
                        categoryIcon = com.minami_studio.kiro.ui.components.resolveIcon(mapIconName) ?: mapEntry.category.materialIcon,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // 底部 Tab 栏（含居中 + 按钮，与 iOS 一致）
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 4.dp,
            color = WanderWarm.copy(alpha = 0.95f)
        ) {
            key(language) { Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TabBarItem(
                        icon = Tab.home.icon,
                        label = langManager.s.tabHome,
                        isSelected = selectedTab == Tab.home,
                        onClick = {
                            selectedTab = Tab.home
                            navController.navigate("home") { popUpTo("home") { inclusive = true } }
                        }
                    )
                }
                // Map
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TabBarItem(
                        icon = Tab.map.icon,
                        label = langManager.s.tabMap,
                        isSelected = selectedTab == Tab.map,
                        onClick = {
                            selectedTab = Tab.map
                            navController.navigate("map") { popUpTo("home") }
                        }
                    )
                }
                // + 按钮（居中，上浮）
                Box(
                    modifier = Modifier
                        .offset(y = (-12).dp)
                        .size(52.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(WanderInk)
                        .clickable { showAddEntry = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = WanderCream,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Collection
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TabBarItem(
                        icon = Tab.collection.icon,
                        label = langManager.s.tabCollection,
                        isSelected = selectedTab == Tab.collection,
                        onClick = {
                            selectedTab = Tab.collection
                            navController.navigate("collection") { popUpTo("home") }
                        }
                    )
                }
                // Profile
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TabBarItem(
                        icon = Tab.profile.icon,
                        label = langManager.s.tabProfile,
                        isSelected = selectedTab == Tab.profile,
                        onClick = {
                            selectedTab = Tab.profile
                            navController.navigate("profile") { popUpTo("home") }
                        }
                    )
                }
            } }
        }

        // 新建条目底部面板（全屏 Dialog，与 iOS sheet 一致）
        if (showAddEntry) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddEntry = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WanderWarm)
                ) {
                    AddEntryScreen(
                        entryStore = entryStore,
                        langManager = langManager,
                        onDismiss = { showAddEntry = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) WanderAccent else WanderMuted
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
