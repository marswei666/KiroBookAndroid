package com.minami_studio.kiro.ui.profile

import android.content.Intent
import com.minami_studio.kiro.BuildConfig
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.minami_studio.kiro.R
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.repository.PhotoRepository
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.data.subscription.SubscriptionManager
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.subscription.SubscriptionManagementSection
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    entries: List<Entry>,
    customCategories: List<CustomCategory>,
    language: AppLanguage,
    entryStore: EntryStore,
    langManager: LanguageManager,
    subscriptionManager: SubscriptionManager? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var profileName by remember { mutableStateOf(prefs.getString("profile_name", "") ?: "") }
    var tagline by remember { mutableStateOf(prefs.getString("profile_tagline", "") ?: "") }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingTagline by remember { mutableStateOf(false) }

    fun saveProfile() {
        prefs.edit()
            .putString("profile_name", profileName)
            .putString("profile_tagline", tagline)
            .apply()
    }

    LaunchedEffect(isEditingName) { if (!isEditingName) saveProfile() }
    LaunchedEffect(isEditingTagline) { if (!isEditingTagline) saveProfile() }

    var showExportSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }

    // 头像（与 iOS 一致：加载为内存 Bitmap，选图后立即更新）
    fun loadAvatarBitmap(): android.graphics.Bitmap? {
        val file = PhotoRepository.getAvatarFile(context)
        return if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }
    var avatarBitmap by remember { mutableStateOf(loadAvatarBitmap()) }
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            PhotoRepository.saveAvatar(context, uri)
            avatarBitmap = loadAvatarBitmap()
        }
    }

    // 导入
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val count = entryStore.importZip(uri)
                profileName = prefs.getString("profile_name", "") ?: ""
                tagline = prefs.getString("profile_tagline", "") ?: ""
                importResult = when {
                    count > 0 -> langManager.s.importSuccess(count)
                    count == 0 -> langManager.s.importNoNew
                    else -> langManager.s.importErrCannotRead
                }
            } catch (e: Exception) {
                importResult = "${langManager.s.importErrReadFailed}：${e.message}"
            }
        }
    }

    // 统计
    val totalCheckIns = entries.size
    val uniqueCities = entries.map { it.city }.filter { it.isNotEmpty() }.distinct()
    val uniqueCountries = entries.map { it.country }.filter { it.isNotEmpty() }.distinct()

    // 品类分布（与 iOS 一致：先自定义品类，再标准品类）
    val categoryBreakdown = remember(entries, customCategories) {
        val result = mutableListOf<Triple<String, ImageVector?, Int>>()
        for (cat in customCategories) {
            val count = entries.count { it.customCategoryID == cat.id }
            if (count > 0) result.add(Triple(cat.displayName(language.code), resolveIcon(cat.icon), count))
        }
        for (cat in PlaceCategory.entries) {
            val count = entries.count { it.customCategoryID == null && it.category == cat }
            if (count > 0) result.add(Triple(cat.localizedName(language.code), cat.materialIcon, count))
        }
        result.sortedByDescending { it.third }
    }

    val visitedCountries = remember(entries) {
        entries.map { it.country }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    val photoSize = entryStore.totalPhotoSize()
    val sizeStr = when {
        photoSize > 1024 * 1024 -> "%.1f MB".format(photoSize / 1024.0 / 1024.0)
        photoSize > 1024 -> "%.1f KB".format(photoSize / 1024.0)
        else -> "$photoSize B"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanderWarm)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Spacer(modifier = Modifier.height(20.dp))

        // ========== 头部：头像 + 名称 + 标语（与 iOS 一致） ==========
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 头像（带相机徽章）
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3D2010), Color(0xFF8B6040))
                            )
                        )
                        .clickable { avatarPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("✈️", fontSize = 36.sp)
                    }
                }
                // 相机徽章（与 iOS 一致：wanderAccent 背景，11dp 图标）
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(WanderAccent)
                        .clickable { avatarPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        "Change avatar",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // 名称（可编辑，无边框 BasicTextField，与 iOS 一致：按回车/完成键退出编辑）
            if (isEditingName) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                BasicTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = WanderInk,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = Brush.linearGradient(listOf(WanderAccent, WanderAccent)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        isEditingName = false
                    })
                )
            } else {
                Text(
                    text = profileName.ifEmpty { langManager.s.traveler },
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    color = WanderInk,
                    modifier = Modifier.clickable {
                        isEditingName = true
                        isEditingTagline = false
                    }
                )
            }

            // 标语（可编辑，无边框 BasicTextField，与 iOS 一致：按回车/完成键退出编辑）
            if (isEditingTagline) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                BasicTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = WanderMuted,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = Brush.linearGradient(listOf(WanderAccent, WanderAccent)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        isEditingTagline = false
                    })
                )
            } else {
                Text(
                    text = tagline.ifEmpty { langManager.s.profileTagline },
                    fontSize = 13.sp,
                    color = WanderMuted,
                    modifier = Modifier.clickable {
                        isEditingTagline = true
                        isEditingName = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 大统计卡片（与 iOS BigStatCard 一致：图标 + 数字 + 标签） ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigStatCard(
                count = totalCheckIns,
                label = langManager.s.totalCheckIns,
                icon = Icons.Default.Place,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                count = uniqueCities.size,
                label = langManager.s.cities,
                icon = Icons.Default.Business,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                count = uniqueCountries.size,
                label = langManager.s.countries,
                icon = Icons.Default.Public,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 品类分布卡片（与 iOS 一致：图标 + 进度条） ==========
        if (categoryBreakdown.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        langManager.s.categoryBreakdown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = WanderMuted
                    )
                    categoryBreakdown.forEach { (name, icon, count) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (icon != null) {
                                        Icon(icon, null, tint = WanderInk, modifier = Modifier.size(10.dp))
                                    }
                                    Text(name, fontSize = 14.sp, color = WanderInk)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "$count",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WanderMuted
                                )
                            }
                            // 进度条（与 iOS 一致：wanderBlush 背景 + wanderAccent 前景）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(WanderBlush)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = count.toFloat() / totalCheckIns.coerceAtLeast(1))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(WanderAccent)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ========== 去过的国家卡片（与 iOS 一致：胶囊标签） ==========
        if (visitedCountries.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        langManager.s.visitedCountries(visitedCountries.size),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = WanderMuted
                    )
                    // 流式布局（简化版）
                    val rows = visitedCountries.chunked(4)
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { country ->
                                Text(
                                    country,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WanderInk,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(WanderBlush)
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ========== 存储卡片（与 iOS 一致：照片占用 + 隐私说明） ==========
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    langManager.s.storage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = WanderMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        tint = WanderAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(langManager.s.photoStorage, fontSize = 14.sp, color = WanderInk)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(sizeStr, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderMuted)
                }
                Text(
                    langManager.s.privacyNote,
                    fontSize = 12.sp,
                    color = WanderMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 操作卡片（与 iOS ActionRow 一致：图标 + 文字 + 箭头） ==========
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                ActionRow(
                    icon = Icons.Default.Upload,
                    label = langManager.s.exportBackup,
                    onClick = { showExportSheet = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = WanderBlush
                )
                ActionRow(
                    icon = Icons.Default.Download,
                    label = langManager.s.importBackup,
                    onClick = { showImportSheet = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = WanderBlush
                )
                ActionRow(
                    icon = Icons.Default.Info,
                    label = langManager.s.aboutWander,
                    onClick = { showAboutSheet = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 订阅管理卡片（仅付费用户显示） ==========
        if (subscriptionManager != null) {
            val subscriptionState by subscriptionManager.subscriptionState.collectAsState()
            if (subscriptionState.isPaid) {
                SubscriptionManagementSection(
                    subscriptionManager = subscriptionManager,
                    currentEntryCount = entries.size,
                    strings = langManager.s
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // 导出备份（与 iOS ExportView 一致：全屏 Dialog）
    if (showExportSheet) {
        Dialog(
            onDismissRequest = { showExportSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WanderWarm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 关闭按钮
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WanderBlush)
                                .clickable { showExportSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, langManager.s.close, tint = WanderInk, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Icon(Icons.Default.Upload, null, tint = WanderAccent, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(langManager.s.exportTitle, fontSize = 24.sp, fontFamily = FontFamily.Serif, color = WanderInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        langManager.s.exportDesc,
                        fontSize = 14.sp, color = WanderMuted, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(WanderBlush.copy(alpha = 0.5f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(langManager.s.exportEntriesCount(totalCheckIns), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                        Text(langManager.s.exportPhotoSize(sizeStr), fontSize = 13.sp, color = WanderMuted)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WanderInk)
                            .clickable {
                                val zipUri = entryStore.exportZip()
                                if (zipUri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, zipUri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Kiro Book Backup")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, langManager.s.shareBackup))
                                }
                                showExportSheet = false
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(langManager.s.exportButton, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WanderCream)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // 导入备份（与 iOS ImportView 一致：全屏 Dialog）
    if (showImportSheet) {
        Dialog(
            onDismissRequest = { showImportSheet = false; importResult = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WanderWarm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WanderBlush)
                                .clickable { showImportSheet = false; importResult = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, langManager.s.close, tint = WanderInk, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Icon(Icons.Default.Download, null, tint = WanderAccent, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(langManager.s.importTitle, fontSize = 24.sp, fontFamily = FontFamily.Serif, color = WanderInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        langManager.s.importDesc,
                        fontSize = 14.sp, color = WanderMuted, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )

                    if (importResult != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            importResult!!,
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WanderBlush.copy(alpha = 0.5f))
                                .padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WanderInk)
                            .clickable { importPicker.launch("*/*") }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(langManager.s.importButton, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WanderCream)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // 关于（与 iOS AboutView 一致：全屏 Dialog）
    if (showAboutSheet) {
        Dialog(
            onDismissRequest = { showAboutSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WanderWarm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WanderBlush)
                                .clickable { showAboutSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, langManager.s.close, tint = WanderInk, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text("✦", fontSize = 48.sp, color = WanderAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kiro Book", fontSize = 32.sp, fontFamily = FontFamily.Serif, color = WanderInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(langManager.s.appSubtitle, fontSize = 16.sp, color = WanderMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(langManager.s.version(BuildConfig.VERSION_NAME), fontSize = 12.sp, color = WanderMuted)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 40.dp), color = WanderBlush)
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AboutRow(icon = Icons.Default.Lock, text = langManager.s.aboutPrivacy1)
                        AboutRow(icon = Icons.Default.WifiOff, text = langManager.s.aboutPrivacy2)
                        AboutRow(icon = Icons.Default.PersonOff, text = langManager.s.aboutPrivacy3)
                        AboutRow(
                            icon = Icons.Default.Language,
                            text = "marswei666.com",
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://marswei666.com/index.html")))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = WanderAccent, modifier = Modifier.size(24.dp))
        Text(text, fontSize = 14.sp, color = WanderInk)
    }
}

// ========== BigStatCard（与 iOS 一致：图标 + 数字 + 标签，cardStyle） ==========
@Composable
private fun BigStatCard(
    count: Int,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = WanderAccent, modifier = Modifier.size(18.dp))
        Text(
            count.toString(),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = WanderInk
        )
        Text(label, fontSize = 11.sp, color = WanderMuted)
    }
}

// ========== ActionRow（与 iOS 一致：图标 + 文字 + 箭头） ==========
@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WanderAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = WanderInk)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = WanderMuted,
            modifier = Modifier.size(13.dp)
        )
    }
}
