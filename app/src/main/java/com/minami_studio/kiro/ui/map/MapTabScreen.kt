package com.minami_studio.kiro.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.CustomCategory
import com.minami_studio.kiro.ui.components.CategoryChip
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager
import com.minami_studio.kiro.util.RegionDetector
import com.minami_studio.kiro.data.repository.LocationRepository

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
    val isChina = remember { RegionDetector.isChina(context) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun fetchLocation() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) return

            val locationClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        android.util.Log.d("MapTabScreen", "FusedLocation: WGS84 lat=${loc.latitude}, lng=${loc.longitude}, accuracy=${loc.accuracy}")
                        userLocation = Pair(loc.latitude, loc.longitude)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MapTabScreen", "FusedLocation failed", e)
                }
        } catch (_: Exception) {}
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isChina) {
            AMapContent(
                filteredEntries = filteredEntries,
                entryStore = entryStore,
                userLocation = userLocation,
                onEntryClick = onEntryClick
            )
        } else {
            GoogleMapContent(
                filteredEntries = filteredEntries,
                entryStore = entryStore,
                userLocation = userLocation,
                onEntryClick = onEntryClick
            )
        }

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
                    Text("\uD83D\uDDFA\uFE0F", fontSize = 40.sp)
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

@Composable
private fun GoogleMapContent(
    filteredEntries: List<Entry>,
    entryStore: com.minami_studio.kiro.data.store.EntryStore,
    userLocation: Pair<Double, Double>?,
    onEntryClick: (String) -> Unit
) {
    val defaultPosition = when {
        filteredEntries.isNotEmpty() -> com.google.android.gms.maps.model.LatLng(
            filteredEntries.first().latitude!!,
            filteredEntries.first().longitude!!
        )
        userLocation != null -> com.google.android.gms.maps.model.LatLng(userLocation.first, userLocation.second)
        else -> com.google.android.gms.maps.model.LatLng(35.0, 105.0)
    }

    val cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(defaultPosition, 3f)
    }

    LaunchedEffect(userLocation) {
        if (filteredEntries.isEmpty() && userLocation != null) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLng(
                    com.google.android.gms.maps.model.LatLng(userLocation.first, userLocation.second)
                )
            )
        }
    }

    com.google.maps.android.compose.GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = com.google.maps.android.compose.MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        ),
        properties = com.google.maps.android.compose.MapProperties(
            isMyLocationEnabled = userLocation != null
        )
    ) {
        filteredEntries.forEach { entry ->
            val position = com.google.android.gms.maps.model.LatLng(
                entry.latitude!!, entry.longitude!!
            )
            val iconName = entryStore.categoryIcon(entry)
            val markerIcon = rememberGoogleCategoryMarker(
                resolveIcon(iconName) ?: entry.category.materialIcon
            )
            com.google.maps.android.compose.Marker(
                state = com.google.maps.android.compose.MarkerState(position = position),
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
}

@Composable
private fun AMapContent(
    filteredEntries: List<Entry>,
    entryStore: com.minami_studio.kiro.data.store.EntryStore,
    userLocation: Pair<Double, Double>?,
    onEntryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val gcjLocation = userLocation?.let {
        val converted = LocationRepository.wgs84ToGcj02(it.first, it.second)
        android.util.Log.d("MapTabScreen", "WGS84→GCJ02: (${it.first}, ${it.second}) → (${converted.first}, ${converted.second})")
        converted
    }

    val defaultLat = when {
        filteredEntries.isNotEmpty() -> filteredEntries.first().latitude!!
        gcjLocation != null -> gcjLocation.first
        else -> 35.0
    }
    val defaultLng = when {
        filteredEntries.isNotEmpty() -> filteredEntries.first().longitude!!
        gcjLocation != null -> gcjLocation.second
        else -> 105.0
    }

    val markerBitmaps = remember(filteredEntries) {
        filteredEntries.associate { entry ->
            val iconName = entryStore.categoryIcon(entry)
            val icon = resolveIcon(iconName) ?: entry.category.materialIcon
            entry.id to createCategoryMarkerBitmap(icon)
        }
    }

    AndroidView(
        factory = { ctx ->
            com.amap.api.maps.MapView(ctx).apply {
                onCreate(null)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mv: com.amap.api.maps.MapView ->
            val amap = mv.map ?: return@AndroidView
            amap.uiSettings.isZoomControlsEnabled = false
            amap.uiSettings.isMyLocationButtonEnabled = false

            amap.moveCamera(
                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                    com.amap.api.maps.model.LatLng(defaultLat, defaultLng), 3f
                )
            )

            amap.clear()

            filteredEntries.forEach { entry ->
                val position = com.amap.api.maps.model.LatLng(
                    entry.latitude!!, entry.longitude!!
                )
                val bitmap = markerBitmaps[entry.id] ?: return@forEach
                val amapIcon = com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)

                val markerOptions = com.amap.api.maps.model.MarkerOptions()
                    .position(position)
                    .title(entry.name)
                    .snippet(entry.city)
                    .icon(amapIcon)
                    .anchor(0.5f, 1.0f)
                amap.addMarker(markerOptions)?.setObject(entry.id)
            }

            if (filteredEntries.isEmpty() && gcjLocation != null) {
                amap.animateCamera(
                    com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                        com.amap.api.maps.model.LatLng(gcjLocation.first, gcjLocation.second), 15f
                    )
                )
            }

            amap.setOnMarkerClickListener { marker ->
                val entryId = marker.getObject() as? String
                if (entryId != null) {
                    onEntryClick(entryId)
                }
                true
            }
        },
        onRelease = { it.onDestroy() }
    )
}
