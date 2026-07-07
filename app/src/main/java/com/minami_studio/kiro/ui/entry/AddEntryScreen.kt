package com.minami_studio.kiro.ui.entry

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.minami_studio.kiro.data.model.*
import com.minami_studio.kiro.data.repository.LocationRepository
import com.minami_studio.kiro.data.repository.PhotoRepository
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.ui.components.StarRating
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.ExifUtils
import com.minami_studio.kiro.util.LanguageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddEntryScreen(
    entryStore: EntryStore,
    langManager: LanguageManager,
    onDismiss: () -> Unit,
    editEntry: Entry? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editEntry?.name ?: "") }
    var note by remember { mutableStateOf(editEntry?.note ?: "") }
    var city by remember { mutableStateOf(editEntry?.city ?: "") }
    var country by remember { mutableStateOf(editEntry?.country ?: "") }
    var latitude by remember { mutableStateOf(editEntry?.latitude) }
    var longitude by remember { mutableStateOf(editEntry?.longitude) }
    var rating by remember { mutableIntStateOf(editEntry?.rating ?: 4) }
    var selectedCategory by remember { mutableStateOf(editEntry?.category ?: PlaceCategory.cafe) }
    var selectedCustomCatId by remember { mutableStateOf(editEntry?.customCategoryID) }
    var photos by remember { mutableStateOf(editEntry?.photoFilenames ?: emptyList()) }
    var visitDate by remember { mutableStateOf(editEntry?.visitedAt ?: LocalDate.now().toString()) }
    var isLocating by remember { mutableStateOf(false) }
    var addressQuery by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showNoCoordDialog by remember { mutableStateOf(false) }
    val originalPhotos = remember { editEntry?.photoFilenames?.toSet() ?: emptySet() }

    fun cancelAndCleanup() {
        val newlyAdded = photos.filter { it !in originalPhotos }
        if (newlyAdded.isNotEmpty()) PhotoRepository.delete(context, newlyAdded)
        onDismiss()
    }

    // 品类管理状态
    var showAddCategory by remember { mutableStateOf(false) }
    var showEditCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<com.minami_studio.kiro.data.model.CustomCategory?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val customCategories by entryStore.customCategories.collectAsState()

    // 处理选中的图片：提取 EXIF GPS + 保存
    fun processPhotos(uris: List<Uri>) {
        uris.forEach { uri ->
            val filename = PhotoRepository.savePhoto(context, uri)
            if (filename != null) { photos = photos + filename }
        }
    }

    // 图片选择器
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            processPhotos(uris)
        }
    }

    fun addPhotos() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // 定位权限
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isLocating = true
            scope.launch {
                val latLng = LocationRepository.getCurrentLatLng(context)
                if (latLng != null) {
                    latitude = latLng.first; longitude = latLng.second
                    val geo = LocationRepository.geocode(context, latLng.first, latLng.second, langManager.langCode)
                    if (geo != null) { city = geo.city; country = geo.country }
                }
                isLocating = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WanderWarm)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ========== 标题栏（与 iOS 一致：取消 + 标题 + 保存胶囊） ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                langManager.s.cancel,
                fontSize = 14.sp,
                color = WanderMuted,
                modifier = Modifier.clickable { cancelAndCleanup() }
            )
            Text(
                text = if (editEntry != null) langManager.s.editEntry else langManager.s.newEntry,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = WanderInk
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (name.isNotBlank() && city.isNotBlank() && country.isNotBlank()) WanderInk else WanderMuted)
                    .clickable(enabled = name.isNotBlank() && city.isNotBlank() && country.isNotBlank() && !isSaving) {
                        if (latitude == null) {
                            showNoCoordDialog = true
                        } else {
                            isSaving = true
                            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                            val entry = Entry(
                                id = editEntry?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name, category = if (selectedCustomCatId != null) PlaceCategory.other else selectedCategory, note = note,
                                rating = rating, city = city, country = country,
                                latitude = latitude, longitude = longitude,
                                photoFilenames = photos,
                                isFavorite = editEntry?.isFavorite ?: false,
                                visitedAt = visitDate,
                                createdAt = editEntry?.createdAt ?: now,
                                customCategoryID = selectedCustomCatId
                            )
                            if (editEntry != null) entryStore.update(entry) else entryStore.add(entry)
                            onDismiss()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(langManager.s.save, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 照片区域（与 iOS 一致：横滑 90x90，删除按钮） ==========
        SectionLabel(langManager.s.photos)
        Spacer(modifier = Modifier.height(12.dp))

        var draggingIndex by remember { mutableIntStateOf(-1) }
        var dragOffsetX by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            photos.forEachIndexed { idx, filename ->
                val file = PhotoRepository.getPhotoFile(context, filename)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset { if (idx == draggingIndex) IntOffset(dragOffsetX.toInt(), 0) else IntOffset.Zero }
                        .zIndex(if (idx == draggingIndex) 1f else 0f)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(idx) {
                            val stepPx = with(density) { 100.dp.toPx() }
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingIndex = idx; dragOffsetX = 0f },
                                onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val targetIdx = idx + (dragOffsetX / stepPx).toInt()
                                    val clampedTarget = targetIdx.coerceIn(0, photos.size - 1)
                                    if (clampedTarget != idx && clampedTarget != draggingIndex) {
                                        val mutable = photos.toMutableList()
                                        val item = mutable.removeAt(idx)
                                        mutable.add(clampedTarget, item)
                                        photos = mutable
                                        draggingIndex = clampedTarget
                                        dragOffsetX = 0f
                                    }
                                },
                                onDragEnd = { draggingIndex = -1; dragOffsetX = 0f },
                                onDragCancel = { draggingIndex = -1; dragOffsetX = 0f }
                            )
                        }
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 删除按钮（放在 Box 内，不超出边界）
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { photos = photos - filename },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Remove",
                            tint = WanderInk,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            // 添加照片按钮
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WanderBlush.copy(alpha = 0.5f))
                    .border(
                        width = 1.5.dp,
                        color = WanderAccent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { addPhotos() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = WanderAccent, modifier = Modifier.size(20.dp))
                    Text(langManager.s.add, fontSize = 11.sp, color = WanderAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 品类选择（与 iOS LazyVGrid 一致：4列，选中 wanderInk，未选 white + wanderBlush 边框，长按编辑/删除） ==========
        SectionLabel(langManager.s.category)
        Spacer(modifier = Modifier.height(12.dp))

        // 用 Row + chunked 替代 LazyVerticalGrid，避免嵌套滚动问题
        val allItems: List<com.minami_studio.kiro.data.model.CustomCategory?> = customCategories + listOf<com.minami_studio.kiro.data.model.CustomCategory?>(null)
        val chunkedCats = allItems.chunked(4)
        chunkedCats.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { catNullable ->
                    if (catNullable != null) {
                        val cat = catNullable
                        val isSelected = selectedCustomCatId == cat.id
                        var showMenu by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) WanderInk else Color.White)
                                    .then(
                                        if (!isSelected) Modifier.border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
                                        else Modifier
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            selectedCustomCatId = cat.id
                                            cat.sourcePlaceCategory?.let { selectedCategory = it }
                                        },
                                        onLongClick = { showMenu = true }
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val icon = resolveIcon(cat.icon)
                                    if (icon != null) {
                                        Icon(
                                            icon, null,
                                            tint = if (isSelected) WanderCream else WanderAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        entryStore.displayName(cat, langManager.langCode),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) WanderCream else WanderInk,
                                        maxLines = 1
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, 4.dp),
                                modifier = Modifier.widthIn(min = 150.dp, max = 170.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = WanderWarm,
                                shadowElevation = 8.dp,
                                tonalElevation = 0.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text(langManager.s.edit, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WanderInk) },
                                    onClick = {
                                        showMenu = false
                                        editingCategory = cat
                                        showEditCategory = true
                                    },
                                    trailingIcon = { Icon(Icons.Default.Edit, null, tint = WanderMuted, modifier = Modifier.size(16.dp)) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(color = WanderBlush, modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
                                DropdownMenuItem(
                                    text = { Text(langManager.s.delete, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD93025)) },
                                    onClick = {
                                        showMenu = false
                                        entryStore.deleteCustomCategory(cat)
                                        if (selectedCustomCatId == cat.id) {
                                            selectedCustomCatId = customCategories.firstOrNull { it.id != cat.id }?.id
                                        }
                                    },
                                    trailingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFD93025), modifier = Modifier.size(16.dp)) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        // 添加按钮（与 iOS 一致：虚线边框）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.5.dp,
                                    brush = SolidColor(WanderAccent.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showAddCategory = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Add, null, tint = WanderAccent, modifier = Modifier.size(20.dp))
                                Text(langManager.s.add, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                            }
                        }
                    }
                }
                // 补齐空位
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ========== 位置（与 iOS locationSection 一致：地址搜索 + 城市/国家 + 自动定位 + 店名 + 坐标 + 日期） ==========
        SectionLabel(langManager.s.location)
        Spacer(modifier = Modifier.height(12.dp))

        // 地址搜索行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WanderTextField(
                value = addressQuery,
                onValueChange = { addressQuery = it },
                placeholder = langManager.s.addressPlaceholder,
                modifier = Modifier.weight(1f),
                imeAction = ImeAction.Done,
                onDone = {
                    if (addressQuery.isNotBlank()) {
                        isGeocoding = true
                        scope.launch {
                            val geo = LocationRepository.searchAddress(context, addressQuery, langManager.langCode)
                            if (geo != null) {
                                latitude = geo.latitude; longitude = geo.longitude
                                city = geo.city; country = geo.country
                            } else {
                                android.widget.Toast.makeText(context, langManager.s.addressNotFound, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            isGeocoding = false
                        }
                    }
                }
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
                    .clickable {
                        if (addressQuery.isNotBlank() && !isGeocoding) {
                            isGeocoding = true
                            scope.launch {
                                val geo = LocationRepository.searchAddress(context, addressQuery, langManager.langCode)
                                if (geo != null) {
                                    latitude = geo.latitude; longitude = geo.longitude
                                    city = geo.city; country = geo.country
                                } else {
                                    android.widget.Toast.makeText(context, langManager.s.addressNotFound, android.widget.Toast.LENGTH_SHORT).show()
                                }
                                isGeocoding = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isGeocoding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = WanderAccent
                    )
                } else {
                    Icon(Icons.Default.Search, null, tint = WanderAccent, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 城市 / 国家 + 自动定位
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WanderTextField(
                value = city,
                onValueChange = { city = it },
                placeholder = langManager.s.city,
                required = true,
                modifier = Modifier.weight(1f)
            )
            WanderTextField(
                value = country,
                onValueChange = { country = it },
                placeholder = langManager.s.country,
                required = true,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(enabled = !isLocating) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        isLocating = true
                        scope.launch {
                            val latLng = LocationRepository.getCurrentLatLng(context)
                            if (latLng != null) {
                                latitude = latLng.first; longitude = latLng.second
                                val geo = LocationRepository.geocode(context, latLng.first, latLng.second, langManager.langCode)
                                if (geo != null) { city = geo.city; country = geo.country }
                            }
                            isLocating = false
                        }
                    } else {
                        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = WanderAccent
                    )
                } else {
                    Icon(
                        Icons.Default.MyLocation, null,
                        tint = WanderAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    if (isLocating) langManager.s.locating else langManager.s.autoLocate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WanderAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 店名（与 iOS 位置区域内一致）
        RequiredSectionLabel(langManager.s.shopName)
        Spacer(modifier = Modifier.height(8.dp))
        WanderTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = langManager.s.shopNamePlaceholder,
            modifier = Modifier.fillMaxWidth()
        )

        // 坐标获取提示
        if (latitude != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, null, tint = WanderAccent, modifier = Modifier.size(10.dp))
                Text(langManager.s.coordinateObtained, fontSize = 11.sp, color = WanderAccent)
            }
        }

        // 探访日期（与 iOS DatePicker 一致：点击弹出日历选择器）
        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel(langManager.s.visitDate)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
                .clickable { showDatePicker = true }
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    visitDate.ifEmpty { langManager.s.visitDate },
                    fontSize = 14.sp,
                    color = if (visitDate.isNotEmpty()) WanderInk else WanderMuted
                )
                Icon(Icons.Default.CalendarMonth, null, tint = WanderAccent, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 评分（与 iOS 一致：白色卡片，22dp 星星） ==========
        SectionLabel(langManager.s.rating)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            StarRating(rating = rating, onRatingChanged = { rating = it }, size = 22)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== 感受（与 iOS 一致：白色背景，wanderBlush 边框，placeholder） ==========
        SectionLabel(langManager.s.myNotes)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
        ) {
            if (note.isEmpty()) {
                Text(
                    langManager.s.notesPlaceholder,
                    fontSize = 14.sp,
                    color = WanderMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = WanderInk),
                cursorBrush = SolidColor(WanderAccent)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // ========== 添加品类弹窗 ==========
    if (showAddCategory) {
        IconPickerSheet(
            title = langManager.s.addCategory,
            initialName = "",
            initialIcon = "label",
            langManager = langManager,
            onConfirm = { iconName, icon ->
                val trimmed = iconName.trim()
                if (trimmed.isEmpty()) return@IconPickerSheet
                // 先用当前语言创建，立即选中，后台翻译其他4门语言
                val cat = entryStore.addCustomCategory(
                    name = trimmed,
                    icon = icon,
                    localizedNames = mapOf(langManager.langCode to trimmed)
                )
                selectedCustomCatId = cat.id
                showAddCategory = false
                scope.launch {
                    val isChina = com.minami_studio.kiro.util.RegionDetector.isChina(context)
                    val translations = com.minami_studio.kiro.util.TranslationService
                        .translateToAllLanguages(trimmed, langManager.langCode, isChina)
                    if (translations.size > 1) {
                        entryStore.updateCustomCategory(cat.copy(localizedNames = translations))
                    }
                }
            },
            onDismiss = { showAddCategory = false }
        )
    }

    // ========== 编辑品类弹窗（与 iOS IconPickerSheet 一致） ==========
    if (showEditCategory && editingCategory != null) {
        val cat = editingCategory!!
        IconPickerSheet(
            title = langManager.s.editCategory,
            initialName = entryStore.displayName(cat, langManager.langCode),
            initialIcon = cat.icon,
            langManager = langManager,
            onConfirm = { newName, newIcon ->
                val langKey = langManager.langCode
                val updatedCat = if (cat.sourcePlaceCategory != null) {
                    // 默认品类：仅覆盖当前语言
                    val defaultName = cat.sourcePlaceCategory!!.localizedName(langManager.langCode)
                    val newLocalizedNames = cat.localizedNames.toMutableMap()
                    if (newName == defaultName) {
                        newLocalizedNames.remove(langKey)
                    } else {
                        newLocalizedNames[langKey] = newName
                    }
                    cat.copy(icon = newIcon, localizedNames = newLocalizedNames)
                } else {
                    // 纯自定义品类：更新当前语言覆盖
                    val newLocalizedNames = cat.localizedNames.toMutableMap()
                    newLocalizedNames[langKey] = newName
                    cat.copy(icon = newIcon, localizedNames = newLocalizedNames)
                }
                entryStore.updateCustomCategory(updatedCat)
                editingCategory = null
            },
            onDismiss = {
                showEditCategory = false
                editingCategory = null
            }
        )
    }

    // ========== 日期选择器（自定义日历挂件，完全多语言支持） ==========
    if (showDatePicker) {
        com.minami_studio.kiro.ui.home.SimpleDatePickerDialog(
            langManager = langManager,
            initialDate = try { java.time.LocalDate.parse(visitDate) } catch (e: Exception) { java.time.LocalDate.now() },
            onConfirm = { date -> visitDate = date.toString(); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    // ========== 未获取经纬度提示弹框 ==========
    if (showNoCoordDialog) {
        AlertDialog(
            onDismissRequest = { showNoCoordDialog = false },
            containerColor = WanderWarm,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    langManager.s.noCoordTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
            },
            text = {
                Text(
                    langManager.s.noCoordMessage,
                    fontSize = 14.sp,
                    color = WanderMuted
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoCoordDialog = false }) {
                    Text(langManager.s.goBack, color = WanderAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

}

// ========== SectionLabel（与 iOS sectionLabel 一致：11sp semibold tracking 1 uppercase muted） ==========
@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = WanderMuted
    )
}

@Composable
private fun RequiredSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = WanderMuted
        )
        Text(
            " *",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Red
        )
    }
}

// ========== WanderTextField（与 iOS WanderTextFieldStyle 一致：白色背景，14dp 圆角，wanderBlush 边框） ==========
@Composable
private fun WanderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        if (value.isEmpty()) {
            if (required) {
                Text(
                    text = buildAnnotatedString {
                        append(placeholder)
                        withStyle(SpanStyle(color = Color.Red)) { append(" *") }
                    },
                    fontSize = 14.sp,
                    color = WanderMuted
                )
            } else {
                Text(placeholder, fontSize = 14.sp, color = WanderMuted)
            }
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp, color = WanderInk),
            singleLine = true,
            cursorBrush = SolidColor(WanderAccent),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() })
        )
    }
}
