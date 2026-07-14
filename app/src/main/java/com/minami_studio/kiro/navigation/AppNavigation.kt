package com.minami_studio.kiro.navigation

import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.data.subscription.SubscriptionManager
import com.minami_studio.kiro.data.subscription.SubscriptionManagerFactory
import com.minami_studio.kiro.data.subscription.SubscriptionTier
import com.minami_studio.kiro.ui.collection.CollectionScreen
import com.minami_studio.kiro.ui.entry.AddEntryScreen
import com.minami_studio.kiro.ui.entry.EntryDetailScreen
import com.minami_studio.kiro.ui.home.HomeScreen
import com.minami_studio.kiro.ui.map.FullMapScreen
import com.minami_studio.kiro.ui.map.MapTabScreen
import com.minami_studio.kiro.ui.profile.ProfileScreen
import com.minami_studio.kiro.ui.subscription.RestoreSubscriptionDialog
import com.minami_studio.kiro.ui.subscription.SubscriptionUpgradeDialog
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab(val icon: ImageVector, val labelRes: String) {
    home(Icons.Default.Home, "tab_home"),
    map(Icons.Default.Map, "tab_map"),
    collection(Icons.Default.Bookmark, "tab_collection"),
    profile(Icons.Default.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(onAppResume: ((() -> Unit) -> Unit) = {}) {
    val context = LocalContext.current
    val entryStore = remember { EntryStore(context) }
    val langManager = remember { LanguageManager(context) }
    val subscriptionManager = remember { SubscriptionManagerFactory.create() }

    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(Tab.home) }
    var showAddEntry by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var requiredTier by remember { mutableStateOf(SubscriptionTier.FREE) }

    val entries by entryStore.entries.collectAsState()
    val customCategories by entryStore.customCategories.collectAsState()
    val language by langManager.language.collectAsState()

    // 初始化订阅管理器
    LaunchedEffect(Unit) {
        subscriptionManager.initialize(context)
    }

    // 注册 onResume 回调 - 每次从后台返回时刷新订阅状态
    LaunchedEffect(subscriptionManager) {
        onAppResume {
            Log.d("AppNavigation", "onResume - refreshing subscription state")
            GlobalScope.launch { subscriptionManager.restorePurchases() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) {
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize()) {
            composable("home") {
                HomeScreen(entries = entries, customCategories = customCategories, language = language, langManager = langManager, entryStore = entryStore, onEntryClick = { navController.navigate("entry_detail/$it") })
            }
            composable("map") {
                MapTabScreen(entries = entries, customCategories = customCategories, language = language, langManager = langManager, entryStore = entryStore, onEntryClick = { navController.navigate("entry_detail/$it") })
            }
            composable("collection") {
                CollectionScreen(entries = entries, customCategories = customCategories, language = language, langManager = langManager, entryStore = entryStore, onEntryClick = { navController.navigate("entry_detail/$it") })
            }
            composable("profile") {
                ProfileScreen(entries = entries, customCategories = customCategories, language = language, entryStore = entryStore, langManager = langManager, subscriptionManager = subscriptionManager, onShowRestore = { showRestoreDialog = true })
            }
            composable("entry_detail/{entryId}", arguments = listOf(navArgument("entryId") { type = NavType.StringType })) {
                val entryId = it.arguments?.getString("entryId") ?: ""
                val entry = entries.firstOrNull { e -> e.id == entryId }
                if (entry != null) {
                    EntryDetailScreen(entry = entry, entryStore = entryStore, language = language, langManager = langManager, onBack = { navController.popBackStack() }, onEdit = { navController.navigate("edit_entry/${entry.id}") }, onFullMap = { lat, lng -> navController.navigate("full_map/${entry.id}") })
                }
            }
            composable("edit_entry/{entryId}", arguments = listOf(navArgument("entryId") { type = NavType.StringType })) {
                val entryId = it.arguments?.getString("entryId") ?: ""
                val editEntry = entries.firstOrNull { e -> e.id == entryId }
                if (editEntry != null) {
                    Dialog(onDismissRequest = { navController.popBackStack() }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                        Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) {
                            AddEntryScreen(entryStore = entryStore, langManager = langManager, editEntry = editEntry, onDismiss = { navController.popBackStack() })
                        }
                    }
                }
            }
            composable("full_map/{entryId}", arguments = listOf(navArgument("entryId") { type = NavType.StringType })) {
                val entryId = it.arguments?.getString("entryId") ?: ""
                val mapEntry = entries.firstOrNull { e -> e.id == entryId }
                if (mapEntry != null && mapEntry.latitude != null && mapEntry.longitude != null) {
                    val mapIconName = entryStore.categoryIcon(mapEntry)
                    FullMapScreen(latitude = mapEntry.latitude, longitude = mapEntry.longitude, name = mapEntry.name, city = mapEntry.city, country = mapEntry.country, categoryIcon = com.minami_studio.kiro.ui.components.resolveIcon(mapIconName) ?: mapEntry.category.materialIcon, onBack = { navController.popBackStack() })
                }
            }
        }
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), shadowElevation = 4.dp, color = WanderWarm.copy(alpha = 0.95f)) {
            key(language) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TabBarItem(icon = Tab.home.icon, label = langManager.s.tabHome, isSelected = selectedTab == Tab.home, onClick = { selectedTab = Tab.home; navController.navigate("home") { popUpTo("home") { inclusive = true } } }) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TabBarItem(icon = Tab.map.icon, label = langManager.s.tabMap, isSelected = selectedTab == Tab.map, onClick = { selectedTab = Tab.map; navController.navigate("map") { popUpTo("home") } }) }
                    Box(modifier = Modifier.offset(y = (-12).dp).size(52.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(WanderInk).clickable {
                        if (subscriptionManager.canAddEntry(entries.size)) showAddEntry = true
                        else { requiredTier = subscriptionManager.requiredTierForEntryCount(entries.size + 1); showUpgradeDialog = true }
                    }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = "Add", tint = WanderCream, modifier = Modifier.size(20.dp)) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TabBarItem(icon = Tab.collection.icon, label = langManager.s.tabCollection, isSelected = selectedTab == Tab.collection, onClick = { selectedTab = Tab.collection; navController.navigate("collection") { popUpTo("home") } }) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { TabBarItem(icon = Tab.profile.icon, label = langManager.s.tabProfile, isSelected = selectedTab == Tab.profile, onClick = { selectedTab = Tab.profile; navController.navigate("profile") { popUpTo("home") } }) }
                }
            }
        }
        if (showAddEntry) {
            Dialog(onDismissRequest = { showAddEntry = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) { AddEntryScreen(entryStore = entryStore, langManager = langManager, onDismiss = { showAddEntry = false }) }
            }
        }
        if (showUpgradeDialog) {
            SubscriptionUpgradeDialog(currentEntryCount = entries.size, requiredTier = requiredTier, strings = langManager.s, onUpgrade = { tier ->
                val activity = context as? android.app.Activity
                Log.d("AppNavigation", "onUpgrade: activity=$activity, tier=${tier.id}")
                if (activity != null) {
                    GlobalScope.launch {
                        try {
                            val result = subscriptionManager.startPurchase(activity, tier)
                            Log.d("AppNavigation", "startPurchase result: $result")
                            if (result.isSuccess) {
                                withContext(Dispatchers.Main) {
                                    showUpgradeDialog = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    val errorMsg = if (subscriptionManager.isChannelPlay()) langManager.s.playPurchaseFailed else langManager.s.networkTimeoutError
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "Purchase failed: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                val errorMsg = if (subscriptionManager.isChannelPlay()) langManager.s.playPurchaseFailed else langManager.s.networkTimeoutError
                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Log.e("AppNavigation", "Activity is null!")
                    showUpgradeDialog = false
                }
            }, onDismiss = { showUpgradeDialog = false })
        }
        if (showRestoreDialog) {
            RestoreSubscriptionDialog(strings = langManager.s, onSendCode = { subscriptionManager.sendVerificationCode(it).success }, onVerify = { email, code -> subscriptionManager.verifyAndBind(email, code).isSuccess }, onForceUnbind = { subscriptionManager.forceUnbindByEmail(it).success }, onDismiss = { showRestoreDialog = false })
        }
    }
}

@Composable
private fun TabBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) WanderAccent else WanderMuted
    Column(modifier = Modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = color, fontSize = 10.sp, maxLines = 1, softWrap = false)
    }
}
