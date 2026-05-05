package com.minami_studio.kiro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.repository.PhotoRepository
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.ui.components.CategoryChip
import com.minami_studio.kiro.ui.components.StatPill
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    entries: List<Entry>,
    customCategories: List<CustomCategory>,
    language: AppLanguage,
    langManager: LanguageManager,
    entryStore: EntryStore,
    onEntryClick: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showDaySheet by remember { mutableStateOf(false) }
    var daySheetEntries by remember { mutableStateOf<List<Entry>>(emptyList()) }

    // 语言切换下拉
    var showLanguageMenu by remember { mutableStateOf(false) }

    // 统计数据
    val totalCheckIns = entries.size
    val uniqueCities = entries.map { it.city }.filter { it.isNotEmpty() }.distinct().size
    val uniqueCountries = entries.map { it.country }.filter { it.isNotEmpty() }.distinct().size

    // 筛选后的条目
    val filteredEntries = if (selectedCategory != null) {
        entries.filter { entry ->
            val cat = entryStore.customCategoryFor(entry)
            cat?.id == selectedCategory
        }
    } else entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanderWarm)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Spacer(modifier = Modifier.height(20.dp))

        // 品牌 + 语言切换
        val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
        val headerName = prefs.getString("profile_name", "") ?: ""
        val headerTagline = prefs.getString("profile_tagline", "") ?: ""

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 品牌行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦ Kiro Book",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    color = WanderAccent
                )
                // 语言下拉（iOS 风格小按钮）
                Box {
                    Row(
                        modifier = Modifier.clickable { showLanguageMenu = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = language.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WanderMuted
                        )
                        Icon(
                            if (showLanguageMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            "Language",
                            tint = WanderMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = WanderWarm,
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 130.dp)
                    ) {
                        AppLanguage.entries.forEachIndexed { idx, lang ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        langManager.setLanguage(lang)
                                        showLanguageMenu = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 13.sp,
                                        color = if (language == lang) WanderAccent else WanderInk
                                    )
                                    if (language == lang) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = WanderAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < AppLanguage.entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = WanderBlush
                                )
                            }
                        }
                    }
                }
            }

            // 用户名称
            Text(
                text = headerName.ifEmpty { "Hello" },
                fontSize = 22.sp,
                fontFamily = Georgia,
                color = WanderInk
            )
            // 标语
            Text(
                text = headerTagline.ifEmpty { "Explorer." },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WanderMuted
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 统计胶囊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatPill(
                count = totalCheckIns,
                label = langManager.s.homeCheckIns,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                count = uniqueCities,
                label = langManager.s.cities,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                count = uniqueCountries,
                label = langManager.s.countries,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 品类筛选
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    name = langManager.s.all,
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
            }
            items(customCategories) { cat ->
                CategoryChip(
                    name = cat.displayName(language.code),
                    icon = resolveIcon(cat.icon),
                    isSelected = selectedCategory == cat.id,
                    onClick = {
                        selectedCategory = if (selectedCategory == cat.id) null else cat.id
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (entries.isEmpty()) {
            // 空状态提示
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✈️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = langManager.s.homeNoEntries,
                    fontSize = 20.sp,
                    fontFamily = Georgia,
                    color = WanderInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = langManager.s.homeNoEntriesHint,
                    fontSize = 14.sp,
                    color = WanderMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 日历
            WanderCalendar(
                entries = filteredEntries,
                entryStore = entryStore,
                language = language,
                langManager = langManager,
                modifier = Modifier.padding(horizontal = 16.dp),
                onDayEntriesClick = { dayEntries ->
                    daySheetEntries = dayEntries
                    showDaySheet = true
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp)) // 为底部 Tab 栏留空间
    }

    // 日条目底部面板
    if (showDaySheet && daySheetEntries.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showDaySheet = false },
            containerColor = WanderWarm,
            dragHandle = null,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            DayEntriesSheet(
                entries = daySheetEntries,
                entryStore = entryStore,
                language = language,
                langManager = langManager,
                onEntryClick = { entryId ->
                    showDaySheet = false
                    onEntryClick(entryId)
                },
                onDismiss = { showDaySheet = false }
            )
        }
    }
}

@Composable
private fun DayEntriesSheet(
    entries: List<Entry>,
    entryStore: EntryStore,
    language: AppLanguage,
    langManager: LanguageManager,
    onEntryClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(WanderWarm)
            .padding(top = 16.dp)
    ) {
        // 关闭按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onDismiss) {
                Text(langManager.s.close, color = WanderMuted)
            }
        }

        // 日期标题
        val firstEntry = entries.firstOrNull()
        if (firstEntry != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dateLocale = java.util.Locale(language.code)
                Text(
                    text = try {
                        val date = java.time.LocalDate.parse(firstEntry.visitedAt.substring(0, 10))
                        val fmt = when (language.code) {
                            "zh" -> java.time.format.DateTimeFormatter.ofPattern("M月d日", dateLocale)
                            "ja" -> java.time.format.DateTimeFormatter.ofPattern("M月d日", dateLocale)
                            "ko" -> java.time.format.DateTimeFormatter.ofPattern("M월 d일", dateLocale)
                            else -> java.time.format.DateTimeFormatter.ofPattern("M/d", dateLocale)
                        }
                        date.format(fmt)
                    } catch (e: Exception) { "" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
                Text(
                    text = try {
                        val date = java.time.LocalDate.parse(firstEntry.visitedAt.substring(0, 10))
                        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, dateLocale)
                    } catch (e: Exception) { "" },
                    fontSize = 13.sp,
                    color = WanderMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 卡片列表
        if (entries.size > 1) {
            // 多条记录用 HorizontalPager
            HorizontalPager(
                state = rememberPagerState { entries.size },
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                DayEntryCard(
                    entry = entries[page],
                    onClick = { onEntryClick(entries[page].id) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            DayEntryCard(
                entry = entries.first(),
                onClick = { onEntryClick(entries.first().id) },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun DayEntryCard(
    entry: Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            // 照片区域 3:4
            val photoFile = entry.firstPhotoFilename?.let { PhotoRepository.getPhotoFile(context, it) }
            if (photoFile != null && photoFile.exists()) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = entry.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 渐变占位
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3A2A1A), Color(0xFF8B6040))
                            )
                        )
                )
            }

            // 店名
            Text(
                text = entry.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontFamily = Georgia,
                fontStyle = FontStyle.Italic,
                color = WanderInk
            )
        }
    }
}

private val Georgia = FontFamily.Serif
