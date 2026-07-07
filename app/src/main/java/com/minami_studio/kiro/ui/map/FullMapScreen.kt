package com.minami_studio.kiro.ui.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.RegionDetector

@Composable
fun FullMapScreen(
    latitude: Double,
    longitude: Double,
    name: String,
    city: String = "",
    country: String = "",
    categoryIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isChina = remember { RegionDetector.isChina(context) }
    val locationText = listOf(city, country).filter { it.isNotEmpty() }.joinToString(", ")

    Box(modifier = Modifier.fillMaxSize()) {
        if (isChina) {
            AMapFullContent(latitude, longitude, name, categoryIcon)
        } else {
            GoogleMapFullContent(latitude, longitude, name, categoryIcon)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(WanderWarm)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WanderInk,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
                if (locationText.isNotEmpty()) {
                    Text(
                        text = locationText,
                        fontSize = 12.sp,
                        color = WanderMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(WanderWarm)
                    .clickable {
                        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($name)")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in Maps",
                    tint = WanderAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun GoogleMapFullContent(
    latitude: Double,
    longitude: Double,
    name: String,
    categoryIcon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    val latLng = com.google.android.gms.maps.model.LatLng(latitude, longitude)
    val cameraPositionState = com.google.maps.android.compose.rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(latLng, 16f)
    }

    com.google.maps.android.compose.GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = com.google.maps.android.compose.MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        if (categoryIcon != null) {
            val markerIcon = rememberGoogleCategoryMarker(categoryIcon)
            com.google.maps.android.compose.Marker(
                state = com.google.maps.android.compose.MarkerState(position = latLng),
                title = name,
                icon = markerIcon
            )
        } else {
            com.google.maps.android.compose.Marker(
                state = com.google.maps.android.compose.MarkerState(position = latLng),
                title = name
            )
        }
    }
}

@Composable
private fun AMapFullContent(
    latitude: Double,
    longitude: Double,
    name: String,
    categoryIcon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    val markerBitmap = categoryIcon?.let { rememberCategoryMarkerBitmap(it) }

    AndroidView(
        factory = { ctx ->
            com.amap.api.maps.MapView(ctx).apply {
                onCreate(null)
                map?.let { amap ->
                    val position = com.amap.api.maps.model.LatLng(latitude, longitude)
                    amap.moveCamera(
                        com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(position, 16f)
                    )
                    if (markerBitmap != null) {
                        val amapIcon = com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(markerBitmap)
                        amap.addMarker(
                            com.amap.api.maps.model.MarkerOptions()
                                .position(position)
                                .title(name)
                                .icon(amapIcon)
                                .anchor(0.5f, 1.0f)
                        )
                    } else {
                        amap.addMarker(
                            com.amap.api.maps.model.MarkerOptions()
                                .position(position)
                                .title(name)
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.onDestroy() }
    )
}
