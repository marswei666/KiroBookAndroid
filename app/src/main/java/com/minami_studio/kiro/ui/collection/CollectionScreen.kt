package com.minami_studio.kiro.ui.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.R
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager

enum class CollectionMode {
    byCategory, byCountry, favorites
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    entries: List<Entry>,
    customCategories: List<CustomCategory>,
    language: AppLanguage,
    langManager: LanguageManager,
    entryStore: EntryStore,
    onEntryClick: (String) -> Unit
) {
    var mode by remember { mutableStateOf(CollectionMode.byCategory) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WanderWarm)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Spacer(modifier = Modifier.height(20.dp))

        // 标题（与 iOS 一致：wanderSerif 28, horizontal 24, top 20）
        Text(
            text = langManager.s.collectionTitle,
            modifier = Modifier.padding(horizontal = 24.dp),
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            color = WanderInk
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 分段选择器（与首页品类筛选风格一致：选中 WanderAccent，未选 WanderCream + WanderBlush 边框）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionMode.entries.forEach { m ->
                val isSelected = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) WanderAccent else WanderCream)
                        .then(
                            if (!isSelected) Modifier.border(1.dp, WanderBlush, RoundedCornerShape(20.dp))
                            else Modifier
                        )
                        .clickable { mode = m }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (m) {
                            CollectionMode.byCategory -> langManager.s.byCategory
                            CollectionMode.byCountry -> langManager.s.byCountry
                            CollectionMode.favorites -> langManager.s.favorites
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) WanderInk else WanderMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 内容
        when (mode) {
            CollectionMode.byCategory -> CategoryContent(
                entries = entries,
                customCategories = customCategories,
                language = language,
                langManager = langManager,
                entryStore = entryStore,
                onEntryClick = onEntryClick
            )
            CollectionMode.byCountry -> CountryContent(
                entries = entries,
                entryStore = entryStore,
                langManager = langManager,
                onEntryClick = onEntryClick
            )
            CollectionMode.favorites -> FavoritesContent(
                entries = entries,
                entryStore = entryStore,
                langManager = langManager,
                onEntryClick = onEntryClick
            )
        }
    }
}

@Composable
private fun CategoryContent(
    entries: List<Entry>,
    customCategories: List<CustomCategory>,
    language: AppLanguage,
    langManager: LanguageManager,
    entryStore: EntryStore,
    onEntryClick: (String) -> Unit
) {
    // 先按自定义品类分组，再按标准品类分组（与 iOS 一致）
    val customGroups = remember(entries, customCategories) {
        customCategories.mapNotNull { cat ->
            val catEntries = entries.filter { it.customCategoryID == cat.id }
            if (catEntries.isNotEmpty()) cat to catEntries else null
        }
    }

    val standardGroups = remember(entries) {
        PlaceCategory.entries.mapNotNull { cat ->
            val catEntries = entries.filter { it.customCategoryID == null && it.category == cat }
            if (catEntries.isNotEmpty()) cat to catEntries else null
        }
    }

    if (customGroups.isEmpty() && standardGroups.isEmpty()) {
        EmptyState(iconRes = R.drawable.ic_emoji_folder, message = langManager.s.homeNoEntries)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(customGroups) { (cat, catEntries) ->
            CategoryGroupCard(
                displayName = cat.displayName(language.code),
                icon = resolveIcon(cat.icon),
                entries = catEntries,
                entryStore = entryStore,
                langManager = langManager,
                onEntryClick = onEntryClick
            )
        }
        items(standardGroups) { (cat, catEntries) ->
            CategoryGroupCard(
                displayName = cat.localizedName(language.code),
                icon = cat.materialIcon,
                entries = catEntries,
                entryStore = entryStore,
                langManager = langManager,
                onEntryClick = onEntryClick
            )
        }
    }
}

@Composable
private fun CountryContent(
    entries: List<Entry>,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onEntryClick: (String) -> Unit
) {
    val grouped = remember(entries) {
        entries
            .filter { it.country.isNotEmpty() }
            .groupBy { it.country }
            .toSortedMap()
    }

    if (grouped.isEmpty()) {
        EmptyState(iconRes = R.drawable.ic_emoji_globe, message = langManager.s.emptyCountryHint)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (country, countryEntries) ->
            item {
                CountryGroupCard(
                    country = country,
                    entries = countryEntries,
                    entryStore = entryStore,
                    langManager = langManager,
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    entries: List<Entry>,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onEntryClick: (String) -> Unit
) {
    val favorites = remember(entries) { entries.filter { it.isFavorite } }

    if (favorites.isEmpty()) {
        EmptyState(iconRes = R.drawable.ic_emoji_bookmark, message = langManager.s.emptyFavoritesHint)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites) { entry ->
            FavoriteCard(entry = entry, entryStore = entryStore, langManager = langManager, onClick = { onEntryClick(entry.id) })
        }
    }
}

// ========== 卡片组件（与 iOS cardStyle 一致：白色背景 + 20dp圆角 + 阴影） ==========

@Composable
private fun CategoryGroupCard(
    displayName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    entries: List<Entry>,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onEntryClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        // 头部（可点击展开）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        null,
                        tint = WanderAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${entries.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WanderMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = WanderMuted,
                modifier = Modifier
                    .size(12.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }

        // 展开内容
        if (isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = WanderBlush
            )
            val displayed = if (showAll) entries else entries.take(5)
            displayed.forEach { entry ->
                EntryRowItem(entry = entry, entryStore = entryStore, langManager = langManager, onClick = { onEntryClick(entry.id) })
                if (entry != displayed.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = WanderBlush
                    )
                }
            }
            if (entries.size > 5 && !showAll) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAll = true }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        langManager.s.seeAll(entries.size),
                        fontSize = 13.sp,
                        color = WanderAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryGroupCard(
    country: String,
    entries: List<Entry>,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onEntryClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    val cities = remember(entries) {
        entries.map { it.city }.filter { it.isNotEmpty() }.distinct().take(3).joinToString("、")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    country,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
                if (cities.isNotEmpty()) {
                    Text(
                        cities,
                        fontSize = 12.sp,
                        color = WanderMuted
                    )
                }
            }
            Text(
                langManager.s.entriesCount(entries.size),
                fontSize = 12.sp,
                color = WanderMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = WanderMuted,
                modifier = Modifier
                    .size(12.dp)
                    .rotate(if (isExpanded) 90f else 0f)
            )
        }

        if (isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = WanderBlush
            )
            val displayed = if (showAll) entries else entries.take(5)
            displayed.forEach { entry ->
                EntryRowItem(entry = entry, entryStore = entryStore, langManager = langManager, onClick = { onEntryClick(entry.id) })
                if (entry != displayed.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = WanderBlush
                    )
                }
            }
            if (entries.size > 5 && !showAll) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAll = true }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        langManager.s.seeAll(entries.size),
                        fontSize = 13.sp,
                        color = WanderAccent
                    )
                }
            }
        }
    }
}

// ========== EntryRowItem（与 iOS 一致：缩略图 + 名称 + 品类图标/城市/日期 + 星级） ==========

@Composable
private fun EntryRowItem(
    entry: Entry,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略图（48x48, 10dp圆角）
        val file = entry.firstPhotoFilename?.let { PhotoRepository.getPhotoFile(context, it) }
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3A2A1A), Color(0xFF8B6040))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 名称 + 品类图标/城市/日期 + 星级
        Column(modifier = Modifier.weight(1f)) {
            // 第一行：名称
            Text(
                entry.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = WanderInk,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            // 第二行：品类图标 + 城市 + 日期 + 星级
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 品类图标
                val icon = resolveIcon(entryStore.categoryIcon(entry)) ?: entry.category.materialIcon
                Icon(icon, null, tint = WanderAccent, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(5.dp))
                // 城市
                if (entry.city.isNotEmpty()) {
                    Text(entry.city, fontSize = 11.sp, color = WanderMuted, maxLines = 1)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // 日期
                val dateStr = try {
                    val date = java.time.LocalDate.parse(entry.visitedAt.substring(0, 10))
                    val monthAbbr = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[date.monthValue - 1]
                    "$monthAbbr ${date.dayOfMonth}, ${date.year}"
                } catch (e: Exception) { "" }
                if (dateStr.isNotEmpty()) {
                    Text(dateStr, fontSize = 11.sp, color = WanderMuted)
                }
                Spacer(modifier = Modifier.weight(1f))
                // 星级评分
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (i in 1..5) {
                        Icon(
                            if (i <= entry.rating) Icons.Default.Star else Icons.Outlined.Star,
                            null,
                            tint = if (i <= entry.rating) WanderAccent else WanderBlush,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
        }
    }
}

// ========== 收藏卡片（与 iOS EntryCard 一致：照片 + 暗渐变 + 左下角信息） ==========

@Composable
private fun FavoriteCard(
    entry: Entry,
    entryStore: EntryStore,
    langManager: LanguageManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        // 照片或品类渐变背景
        val file = entry.firstPhotoFilename?.let { PhotoRepository.getPhotoFile(context, it) }
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val catColors = when (entry.category) {
                PlaceCategory.cafe -> listOf(Color(0xFF3D2010), Color(0xFF8B6040))
                PlaceCategory.museum -> listOf(Color(0xFF1A2A3D), Color(0xFF4A6A8A))
                PlaceCategory.bar -> listOf(Color(0xFF2A1A3D), Color(0xFF6A4A7A))
                PlaceCategory.bookstore -> listOf(Color(0xFF3A2A1A), Color(0xFF7A5C3E))
                PlaceCategory.gallery -> listOf(Color(0xFF3A2010), Color(0xFFC4956A))
                PlaceCategory.selectShop -> listOf(Color(0xFF1A1A2A), Color(0xFF4A4A6A))
                PlaceCategory.restaurant -> listOf(Color(0xFF1A3020), Color(0xFF4A7A5A))
                PlaceCategory.other -> listOf(Color(0xFF2A2A2A), Color(0xFF6A6A6A))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(catColors))
            )
        }

        // 暗渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 200f
                    )
                )
        )

        // 左下角信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 品类胶囊
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val icon = resolveIcon(entryStore.categoryIcon(entry)) ?: entry.category.materialIcon
                Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(9.dp))
                Text(
                    entryStore.categoryDisplayName(entry, langManager.langCode),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // 名称
            Text(
                entry.name,
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                maxLines = 2
            )

            // 城市 + 国家
            if (entry.city.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.Place, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(9.dp))
                    val location = listOf(entry.city, entry.country).filter { it.isNotEmpty() }.joinToString(", ")
                    Text(
                        location,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ========== 空状态 ==========

@Composable
private fun EmptyState(iconRes: Int, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = WanderMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
