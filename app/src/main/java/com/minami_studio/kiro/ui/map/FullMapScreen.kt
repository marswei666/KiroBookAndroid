package com.minami_studio.kiro.ui.map

import android.content.Intent
import android.net.Uri
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.minami_studio.kiro.ui.theme.*

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
    val latLng = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 16f)
    }

    val locationText = listOf(city, country).filter { it.isNotEmpty() }.joinToString(", ")

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图（全屏）
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = com.google.maps.android.compose.MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            if (categoryIcon != null) {
                val markerIcon = rememberCategoryMarker(categoryIcon)
                Marker(
                    state = MarkerState(position = latLng),
                    title = name,
                    icon = markerIcon
                )
            } else {
                Marker(
                    state = MarkerState(position = latLng),
                    title = name
                )
            }
        }

        // 顶部栏（与 iOS 一致）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮
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

            // 名称 + 城市
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

            // 跳转到地图 App
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
