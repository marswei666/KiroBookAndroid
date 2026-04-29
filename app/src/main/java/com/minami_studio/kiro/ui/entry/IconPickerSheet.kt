package com.minami_studio.kiro.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.LanguageManager

data class IconGroup(
    val name: String,
    val icons: List<Pair<String, ImageVector>>
)

private val iconGroupData = listOf(
    listOf(
        "coffee" to Icons.Default.Coffee,
        "restaurant" to Icons.Default.Restaurant,
        "wine_bar" to Icons.Default.WineBar,
        "cake" to Icons.Default.Cake,
        "local_cafe" to Icons.Default.LocalCafe,
        "local_bar" to Icons.Default.LocalBar,
        "fastfood" to Icons.Default.Fastfood,
        "icecream" to Icons.Default.Icecream,
    ),
    listOf(
        "museum" to Icons.Default.Museum,
        "palette" to Icons.Default.Palette,
        "menu_book" to Icons.Default.MenuBook,
        "theater_comedy" to Icons.Default.TheaterComedy,
        "photo_camera" to Icons.Default.PhotoCamera,
        "music_note" to Icons.Default.MusicNote,
        "movie" to Icons.Default.Movie,
        "brush" to Icons.Default.Brush,
    ),
    listOf(
        "shopping_bag" to Icons.Default.ShoppingBag,
        "store" to Icons.Default.Store,
        "checkroom" to Icons.Default.Checkroom,
        "diamond" to Icons.Default.Diamond,
        "local_mall" to Icons.Default.LocalMall,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "sell" to Icons.Default.Sell,
        "redeem" to Icons.Default.Redeem,
    ),
    listOf(
        "spa" to Icons.Default.Spa,
        "park" to Icons.Default.Park,
        "fitness_center" to Icons.Default.FitnessCenter,
        "sports_esports" to Icons.Default.SportsEsports,
        "pool" to Icons.Default.Pool,
        "self_improvement" to Icons.Default.SelfImprovement,
        "directions_bike" to Icons.Default.DirectionsBike,
        "flight" to Icons.Default.Flight,
    ),
    listOf(
        "place" to Icons.Default.Place,
        "hotel" to Icons.Default.Hotel,
        "church" to Icons.Default.Church,
        "local_hospital" to Icons.Default.LocalHospital,
        "school" to Icons.Default.School,
        "account_balance" to Icons.Default.AccountBalance,
        "local_library" to Icons.Default.LocalLibrary,
        "beach_access" to Icons.Default.BeachAccess,
    ),
    listOf(
        "label" to Icons.Default.Label,
        "star" to Icons.Default.Star,
        "favorite" to Icons.Default.Favorite,
        "home" to Icons.Default.Home,
        "pets" to Icons.Default.Pets,
        "child_care" to Icons.Default.ChildCare,
        "celebration" to Icons.Default.Celebration,
        "auto_awesome" to Icons.Default.AutoAwesome,
    ),
)

private fun iconGroups(langManager: LanguageManager) = listOf(
    IconGroup(name = langManager.s.iconGroupFood, icons = iconGroupData[0]),
    IconGroup(name = langManager.s.iconGroupCulture, icons = iconGroupData[1]),
    IconGroup(name = langManager.s.iconGroupShopping, icons = iconGroupData[2]),
    IconGroup(name = langManager.s.iconGroupLeisure, icons = iconGroupData[3]),
    IconGroup(name = langManager.s.iconGroupPlaces, icons = iconGroupData[4]),
    IconGroup(name = langManager.s.iconGroupOther, icons = iconGroupData[5]),
)

/**
 * 品类名称 + 图标选择器（与 iOS IconPickerSheet 一致）
 * @param title 标题（"添加品类" 或 "编辑品类"）
 * @param initialName 初始名称（编辑时预填）
 * @param initialIcon 初始图标（编辑时预选）
 * @param onConfirm 确认回调（名称, 图标）
 * @param onDismiss 关闭回调
 */
@Composable
fun IconPickerSheet(
    title: String,
    initialName: String = "",
    initialIcon: String = "label",
    langManager: LanguageManager,
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                    .padding(24.dp)
            ) {
                // 标题栏：取消 + 标题 + 确认
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(langManager.s.cancel, fontSize = 14.sp, color = WanderMuted, modifier = Modifier.clickable { onDismiss() })
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WanderInk)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (name.isNotBlank()) WanderInk else WanderMuted)
                            .clickable(enabled = name.isNotBlank()) {
                                onConfirm(name.trim(), selectedIcon)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(langManager.s.confirm, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 名称输入
                Text(langManager.s.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = WanderMuted)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .border(1.dp, WanderBlush, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    if (name.isEmpty()) {
                        Text(langManager.s.categoryNamePlaceholder, fontSize = 14.sp, color = WanderMuted)
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp, color = WanderInk),
                        singleLine = true,
                        cursorBrush = SolidColor(WanderAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 图标选择
                Text(langManager.s.selectIcon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = WanderMuted)
                Spacer(modifier = Modifier.height(12.dp))

                iconGroups(langManager).forEach { group ->
                    Text(
                        group.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = WanderMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(group.icons) { (iconName, icon) ->
                            val isSelected = iconName == selectedIcon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) WanderInk else WanderBlush)
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon, iconName,
                                    tint = if (isSelected) WanderCream else WanderMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
