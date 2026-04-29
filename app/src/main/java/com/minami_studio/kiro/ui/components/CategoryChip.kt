package com.minami_studio.kiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minami_studio.kiro.ui.theme.*

fun resolveIcon(iconName: String?): ImageVector? {
    if (iconName == null) return null
    return when (iconName) {
        // 餐饮
        "coffee", "cup_and_saucer" -> Icons.Default.Coffee
        "restaurant" -> Icons.Default.Restaurant
        "wine_bar" -> Icons.Default.WineBar
        "cake" -> Icons.Default.Cake
        "local_cafe" -> Icons.Default.LocalCafe
        "local_bar" -> Icons.Default.LocalBar
        "fastfood" -> Icons.Default.Fastfood
        "icecream" -> Icons.Default.Icecream
        // 文化
        "museum" -> Icons.Default.Museum
        "palette" -> Icons.Default.Image
        "menu_book", "book" -> Icons.AutoMirrored.Filled.LibraryBooks
        "theater_comedy" -> Icons.Default.TheaterComedy
        "photo_camera" -> Icons.Default.PhotoCamera
        "music_note" -> Icons.Default.MusicNote
        "movie" -> Icons.Default.Movie
        "brush" -> Icons.Default.Brush
        // 购物
        "shopping_bag" -> Icons.Default.ShoppingBag
        "store" -> Icons.Default.Store
        "checkroom" -> Icons.Default.Checkroom
        "diamond" -> Icons.Default.Diamond
        "local_mall" -> Icons.Default.LocalMall
        "shopping_cart" -> Icons.Default.ShoppingCart
        "sell" -> Icons.Default.Sell
        "redeem" -> Icons.Default.Redeem
        // 休闲
        "spa" -> Icons.Default.Spa
        "park" -> Icons.Default.Park
        "fitness_center" -> Icons.Default.FitnessCenter
        "sports_esports" -> Icons.Default.SportsEsports
        "pool" -> Icons.Default.Pool
        "self_improvement" -> Icons.Default.SelfImprovement
        "directions_bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "flight" -> Icons.Default.Flight
        // 场所
        "place" -> Icons.Default.Place
        "hotel" -> Icons.Default.Hotel
        "church" -> Icons.Default.Church
        "local_hospital" -> Icons.Default.LocalHospital
        "school" -> Icons.Default.School
        "account_balance" -> Icons.Default.AccountBalance
        "local_library" -> Icons.Default.LocalLibrary
        "beach_access" -> Icons.Default.BeachAccess
        // 其他
        "label", "tag" -> Icons.AutoMirrored.Filled.Label
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        "home" -> Icons.Default.Home
        "pets" -> Icons.Default.Pets
        "child_care" -> Icons.Default.ChildCare
        "celebration" -> Icons.Default.Celebration
        "auto_awesome" -> Icons.Default.AutoAwesome
        else -> null
    }
}

@Composable
fun CategoryChip(
    name: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) WanderAccent else WanderCream
    val textColor = if (isSelected) WanderInk else WanderMuted

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (!isSelected) Modifier.border(1.dp, WanderBlush, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = name,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
