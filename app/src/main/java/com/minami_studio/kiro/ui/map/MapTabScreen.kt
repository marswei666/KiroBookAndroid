package com.minami_studio.kiro.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.ui.components.CategoryChip
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager

@Composable
fun MapTabScreen(
    entries: List<Entry>,
    customCategories: List<CustomCategory>,
    language: AppLanguage,
    langManager: LanguageManager,
    entryStore: com.minami_studio.kiro.data.store.EntryStore,
    onEntryClick: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // 请求定位权限并获取位置
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchLocation() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return

            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMaxUpdates(1)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        userLocation = LatLng(loc.latitude, loc.longitude)
                    }
                    locationClient.removeLocationUpdates(this)
                }
            }
            locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchLocation()
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val filteredEntries = if (selectedCategory != null) {
        entries.filter { it.customCategoryID == selectedCategory && it.hasLocation }
    } else {
        entries.filter { it.hasLocation }
    }

    // 默认相机位置：优先用当前位置，否则有打卡数据则定位到打卡点，最后兜底中国中心
    val defaultPosition = when {
        filteredEntries.isNotEmpty() -> LatLng(filteredEntries.first().latitude!!, filteredEntries.first().longitude!!)
        userLocation != null -> userLocation!!
        else -> LatLng(35.0, 105.0)
    }
    val defaultZoom = 3f

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, defaultZoom)
    }

    // 当获取到用户位置且无打卡数据时，移动相机到用户位置
    LaunchedEffect(userLocation) {
        if (filteredEntries.isEmpty() && userLocation != null) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLng(userLocation!!)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图（全屏，忽略所有安全区）
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(isMyLocationEnabled = userLocation != null)
        ) {
            filteredEntries.forEach { entry ->
                val position = LatLng(entry.latitude!!, entry.longitude!!)
                val iconName = entryStore.categoryIcon(entry)
                val markerIcon = rememberCategoryMarker(resolveIcon(iconName) ?: entry.category.materialIcon)
                Marker(
                    state = MarkerState(position = position),
                    title = entry.name,
                    snippet = entry.city,
                    icon = markerIcon,
                    onClick = {
                        onEntryClick(entry.id)
                        false
                    }
                )
            }
        }

        // 顶部标题 + 品类筛选（带渐变背景，与 iOS 一致）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(WanderWarm, WanderWarm.copy(alpha = 0f))
                    )
                )
                .statusBarsPadding()
                .padding(top = 20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = langManager.s.mapTitle,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    color = WanderInk
                )
                Text(
                    text = langManager.s.entriesCount(filteredEntries.size),
                    fontSize = 13.sp,
                    color = WanderMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 品类筛选
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
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
        }

        // 空状态提示（iOS 风格，毛玻璃背景）
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(WanderWarm.copy(alpha = 0.92f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🗺️", fontSize = 40.sp)
                    Text(
                        text = langManager.s.noMapEntries,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif,
                        color = WanderInk
                    )
                    Text(
                        text = langManager.s.noMapEntriesHint,
                        fontSize = 13.sp,
                        color = WanderMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * iOS 风格胶囊 Marker：白色胶囊 + 品类图标 + 底部小圆点
 * 用 Android Canvas API 渲染 ImageVector 路径到 Bitmap
 */
@Composable
fun rememberCategoryMarker(
    icon: androidx.compose.ui.graphics.vector.ImageVector
): com.google.android.gms.maps.model.BitmapDescriptor {
    // 品类选中态：背景 WanderAccent，图标 WanderInk（与 CategoryChip 一致）
    val iconTint = WanderInk
    val iconTintInt = android.graphics.Color.rgb(
        (iconTint.red * 255).toInt(),
        (iconTint.green * 255).toInt(),
        (iconTint.blue * 255).toInt()
    )
    return androidx.compose.runtime.remember(icon) {
        val scale = 3f
        val capsuleW = 30 * scale
        val capsuleH = 30 * scale
        val dotR = 3 * scale
        val shadowPad = 6 * scale
        val totalW = (capsuleW + shadowPad * 2).toInt()
        val totalH = (capsuleH + dotR * 2 + shadowPad * 2 + 4 * scale).toInt()
        val bmp = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val cx = totalW / 2f
        val capsuleTop = shadowPad
        val rect = RectF(
            cx - capsuleW / 2, capsuleTop,
            cx + capsuleW / 2, capsuleTop + capsuleH
        )

        // 阴影
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26000000
            maskFilter = BlurMaskFilter(4 * scale, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawRoundRect(RectF(rect.left, rect.top + 2 * scale, rect.right, rect.bottom + 2 * scale),
            capsuleH / 2, capsuleH / 2, shadowPaint)

        // 胶囊背景（与顶部品类筛选选中态一致）
        val bgColor = WanderAccent
        val bgColorInt = android.graphics.Color.rgb(
            (bgColor.red * 255).toInt(),
            (bgColor.green * 255).toInt(),
            (bgColor.blue * 255).toInt()
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColorInt }
        c.drawRoundRect(rect, capsuleH / 2, capsuleH / 2, bgPaint)

        // 渲染图标路径到胶囊中心
        val iconScale = scale * 0.65f
        val iconW = icon.viewportWidth
        val iconH = icon.viewportHeight
        val offsetX = cx - (iconW * iconScale) / 2
        val offsetY = capsuleTop + (capsuleH - iconH * iconScale) / 2

        c.save()
        c.translate(offsetX, offsetY)
        c.scale(iconScale, iconScale)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK  // renderVectorNode 会用 tintColor 覆盖
            style = Paint.Style.FILL
        }

        renderVectorNode(c, icon.root, iconPaint, iconTintInt)
        c.restore()

        // 底部小圆点
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColorInt }
        c.drawCircle(cx, capsuleTop + capsuleH + dotR + 2 * scale, dotR, dotPaint)

        BitmapDescriptorFactory.fromBitmap(bmp)
    }
}

private fun pathDataToSvg(data: List<androidx.compose.ui.graphics.vector.PathNode>): String {
    val sb = StringBuilder()
    for (node in data) {
        when (node) {
            is androidx.compose.ui.graphics.vector.PathNode.MoveTo ->
                sb.append("M${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeMoveTo ->
                sb.append("m${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.LineTo ->
                sb.append("L${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeLineTo ->
                sb.append("l${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.HorizontalTo ->
                sb.append("H${node.x}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeHorizontalTo ->
                sb.append("h${node.dx}")
            is androidx.compose.ui.graphics.vector.PathNode.VerticalTo ->
                sb.append("V${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeVerticalTo ->
                sb.append("v${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.CurveTo ->
                sb.append("C${node.x1},${node.y1},${node.x2},${node.y2},${node.x3},${node.y3}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeCurveTo ->
                sb.append("c${node.dx1},${node.dy1},${node.dx2},${node.dy2},${node.dx3},${node.dy3}")
            is androidx.compose.ui.graphics.vector.PathNode.ReflectiveCurveTo ->
                sb.append("S${node.x1},${node.y1},${node.x2},${node.y2}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveCurveTo ->
                sb.append("s${node.dx1},${node.dy1},${node.dx2},${node.dy2}")
            is androidx.compose.ui.graphics.vector.PathNode.QuadTo ->
                sb.append("Q${node.x1},${node.y1},${node.x2},${node.y2}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeQuadTo ->
                sb.append("q${node.dx1},${node.dy1},${node.dx2},${node.dy2}")
            is androidx.compose.ui.graphics.vector.PathNode.ReflectiveQuadTo ->
                sb.append("T${node.x},${node.y}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeReflectiveQuadTo ->
                sb.append("t${node.dx},${node.dy}")
            is androidx.compose.ui.graphics.vector.PathNode.ArcTo ->
                sb.append("A${node.horizontalEllipseRadius},${node.verticalEllipseRadius},${node.theta},${if (node.isMoreThanHalf) 1 else 0},${if (node.isPositiveArc) 1 else 0},${node.arcStartX},${node.arcStartY}")
            is androidx.compose.ui.graphics.vector.PathNode.RelativeArcTo ->
                sb.append("a${node.horizontalEllipseRadius},${node.verticalEllipseRadius},${node.theta},${if (node.isMoreThanHalf) 1 else 0},${if (node.isPositiveArc) 1 else 0},${node.arcStartDx},${node.arcStartDy}")
            is androidx.compose.ui.graphics.vector.PathNode.Close ->
                sb.append("Z")
        }
    }
    return sb.toString()
}

private fun renderVectorNode(
    c: Canvas,
    node: androidx.compose.ui.graphics.vector.VectorNode,
    paint: Paint,
    tintColor: Int
) {
    when (node) {
        is androidx.compose.ui.graphics.vector.VectorPath -> {
            val svgPath = pathDataToSvg(node.pathData)
            if (svgPath.isNotEmpty()) {
                androidx.core.graphics.PathParser.createPathFromPathData(svgPath)?.let { androidPath ->
                    // 统一用 tintColor 渲染（Material Icons 原始路径为黑色，靠 tint 改色）
                    paint.color = tintColor
                    if (node.fill != null) {
                        paint.style = Paint.Style.FILL
                        c.drawPath(androidPath, paint)
                    }
                    if (node.stroke != null) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = node.strokeLineWidth
                        c.drawPath(androidPath, paint)
                    }
                }
            }
        }
        is androidx.compose.ui.graphics.vector.VectorGroup -> {
            c.save()
            if (node.translationX != 0f) c.translate(node.translationX, 0f)
            if (node.translationY != 0f) c.translate(0f, node.translationY)
            if (node.scaleX != 1f) c.scale(node.scaleX, 1f)
            if (node.scaleY != 1f) c.scale(1f, node.scaleY)
            for (child in node) {
                renderVectorNode(c, child, paint, tintColor)
            }
            c.restore()
        }
    }
}

