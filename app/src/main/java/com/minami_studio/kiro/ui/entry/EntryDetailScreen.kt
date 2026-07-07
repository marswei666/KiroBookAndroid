package com.minami_studio.kiro.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.minami_studio.kiro.util.RegionDetector
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.repository.PhotoRepository
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.ui.components.StarRating
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager

@Composable
fun EntryDetailScreen(
    entry: Entry,
    entryStore: EntryStore,
    language: AppLanguage,
    langManager: LanguageManager,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onFullMap: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(entry.isFavorite) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 照片轮播（与 iOS 一致，320dp 高度）
            if (entry.photoFilenames.isNotEmpty()) {
                val pagerState = rememberPagerState { entry.photoFilenames.size }
                Box {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) { page ->
                        val file = PhotoRepository.getPhotoFile(context, entry.photoFilenames[page])
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // 底部渐变（与 iOS 一致）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                )
                            )
                    )
                    // 页码指示器
                    if (entry.photoFilenames.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(entry.photoFilenames.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) Color.White
                                            else Color.White.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                // 渐变占位
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3A2A1A), Color(0xFF8B6040))
                            )
                        )
                )
            }

            // 信息区域（与 iOS 一致，padding 24）
            Column(modifier = Modifier.padding(24.dp)) {
                // 品类标签 + 收藏按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cat = entryStore.customCategoryFor(entry)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(WanderBlush)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            resolveIcon(entryStore.categoryIcon(entry)) ?: entry.category.materialIcon,
                            null,
                            tint = WanderAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            cat?.displayName(language.code) ?: entry.category.localizedName(language.code),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WanderAccent
                        )
                    }
                    IconButton(onClick = {
                        isFavorite = !isFavorite
                        entryStore.update(entry.copy(isFavorite = isFavorite))
                    }) {
                        Icon(
                            if (isFavorite) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            "Bookmark",
                            tint = if (isFavorite) WanderAccent else WanderMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 店名
                Text(
                    text = entry.name,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = WanderInk
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 位置 + 日期
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (entry.city.isNotEmpty() || entry.country.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = WanderMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = listOf(entry.city, entry.country).filter { it.isNotEmpty() }.joinToString(", "),
                                fontSize = 13.sp,
                                color = WanderMuted
                            )
                        }
                    }
                    if (entry.visitedAt.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = WanderMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = entry.visitedAt.substring(0, minOf(10, entry.visitedAt.length)),
                                fontSize = 13.sp,
                                color = WanderMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 评分
                StarRating(rating = entry.rating, size = 16)

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = WanderBlush)
                Spacer(modifier = Modifier.height(20.dp))

                // 标签
                if (entry.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        entry.tags.forEach { tag ->
                            Text(
                                "#$tag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = WanderAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(WanderBlush)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 城市 / 国家信息卡（与 iOS 一致）
                if (entry.city.isNotEmpty() || entry.country.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (entry.city.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WanderCream)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    langManager.s.city.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = WanderMuted
                                )
                                Text(entry.city, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                            }
                        }
                        if (entry.country.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WanderCream)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    langManager.s.country.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                    color = WanderMuted
                                )
                                Text(entry.country, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 我的感受
                if (entry.note.isNotEmpty()) {
                    Text(
                        langManager.s.myNotesLabel.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = WanderMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        entry.note,
                        fontSize = 14.sp,
                        color = WanderInk,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WanderCream)
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 迷你地图
                if (entry.hasLocation) {
                    val isChina = remember { RegionDetector.isChina(context) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = WanderMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            langManager.s.location.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = WanderMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onFullMap(entry.latitude!!, entry.longitude!!) }
                    ) {
                        if (isChina) {
                            MiniAmapView(entry, entryStore)
                        } else {
                            val latLng = LatLng(entry.latitude!!, entry.longitude!!)
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(latLng, 15f)
                            }
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = com.google.maps.android.compose.MapUiSettings(
                                    zoomControlsEnabled = false,
                                    scrollGesturesEnabled = false
                                )
                            ) {
                                val iconForMarker = resolveIcon(entryStore.categoryIcon(entry)) ?: entry.category.materialIcon
                                val markerIcon = com.minami_studio.kiro.ui.map.rememberGoogleCategoryMarker(iconForMarker)
                                Marker(state = MarkerState(position = latLng), title = entry.name, icon = markerIcon)
                            }
                        }
                        // 右下角全屏按钮
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onFullMap(entry.latitude!!, entry.longitude!!) }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen, null,
                                tint = Color.White, modifier = Modifier.size(10.dp)
                            )
                            Text(
                                langManager.s.fullscreenView,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // 顶部悬浮按钮（与 iOS 一致，半透明圆形）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 返回按钮
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            // 菜单按钮
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreHoriz, "Menu", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.widthIn(min = 160.dp, max = 180.dp),
                    offset = DpOffset((-4).dp, 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = WanderWarm,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    DropdownMenuItem(
                        text = { Text(langManager.s.edit, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderInk) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = WanderMuted, modifier = Modifier.size(16.dp)) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = WanderBlush)
                    DropdownMenuItem(
                        text = { Text(langManager.s.delete, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD93025)) },
                        onClick = { showMenu = false; showDeleteDialog = true },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFD93025), modifier = Modifier.size(16.dp)) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(langManager.s.deleteEntryTitle) },
            text = { Text(langManager.s.deleteEntryMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryStore.delete(entry)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text(langManager.s.delete, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(langManager.s.cancel)
                }
            }
        )
    }
}

@Composable
private fun MiniAmapView(
    entry: com.minami_studio.kiro.data.model.Entry,
    entryStore: com.minami_studio.kiro.data.store.EntryStore
) {
    val context = LocalContext.current
    val iconForMarker = resolveIcon(entryStore.categoryIcon(entry)) ?: entry.category.materialIcon
    val markerBitmap = com.minami_studio.kiro.ui.map.rememberCategoryMarkerBitmap(iconForMarker)

    AndroidView(
        factory = { ctx ->
            com.amap.api.maps.MapView(ctx).apply {
                onCreate(null)
                map?.let { amap ->
                    amap.uiSettings.isZoomControlsEnabled = false
                    amap.uiSettings.isScrollGesturesEnabled = false

                    val position = com.amap.api.maps.model.LatLng(entry.latitude!!, entry.longitude!!)
                    amap.moveCamera(
                        com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(position, 15f)
                    )

                    val amapIcon = com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(markerBitmap)
                    amap.addMarker(
                        com.amap.api.maps.model.MarkerOptions()
                            .position(position)
                            .title(entry.name)
                            .icon(amapIcon)
                            .anchor(0.5f, 1.0f)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        onReset = null,
        onRelease = { it.onDestroy() }
    )
}
