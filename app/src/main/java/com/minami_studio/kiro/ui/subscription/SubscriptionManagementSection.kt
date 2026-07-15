package com.minami_studio.kiro.ui.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minami_studio.kiro.data.subscription.SubscriptionManager
import com.minami_studio.kiro.data.subscription.SubscriptionState
import com.minami_studio.kiro.data.subscription.SubscriptionTier
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.Strings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun SubscriptionManagementSection(
    subscriptionManager: SubscriptionManager,
    currentEntryCount: Int,
    strings: Strings
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by subscriptionManager.subscriptionState.collectAsState()
    var showUnlinkDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = WanderAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.subManageSubscription, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = WanderMuted)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.subCurrentPlan, fontSize = 12.sp, color = WanderMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(if (state.isFree) strings.subFreePlan else "${state.tier.displayName}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                }

            }
            if (!state.isFree && state.isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.subEntriesUsed(currentEntryCount, state.tier.maxEntries), fontSize = 13.sp, color = WanderMuted)
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.5.dp)).background(WanderBlush)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction = (if (state.tier.maxEntries == Int.MAX_VALUE) 0.5f else currentEntryCount.toFloat() / state.tier.maxEntries).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(2.5.dp)).background(WanderAccent))
                    }
                }
            }
            HorizontalDivider(color = WanderBlush)
            if (state.email.isNotEmpty() && subscriptionManager.isChannelDirect()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, null, tint = WanderAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.subBoundEmail, fontSize = 14.sp, color = WanderInk)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(state.email, fontSize = 13.sp, color = WanderMuted)
                }
            }
            if (state.isPaid && activity != null) {
                SubscriptionActionRow(
                    icon = Icons.Default.OpenInBrowser,
                    label = if (subscriptionManager.isChannelDirect()) strings.subCancelSubscription else strings.subManagePlay,
                    subtitle = if (subscriptionManager.isChannelDirect()) strings.subCancelDesc else strings.subManagePlayDesc,
                    onClick = {
                        GlobalScope.launch { subscriptionManager.openManagementPortal(activity) }
                    }
                )
            }

        }
    }
    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text(strings.subUnlinkDevice) },
            text = { Text(strings.subUnlinkConfirm) },
            confirmButton = {
                TextButton(onClick = { showUnlinkDialog = false; GlobalScope.launch { subscriptionManager.unbindDevice() } }) {
                    Text(strings.confirm, color = Color.Red)
                }
            },
            dismissButton = { TextButton(onClick = { showUnlinkDialog = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun SubscriptionActionRow(icon: ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WanderAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = WanderInk)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = WanderMuted, maxLines = 2, lineHeight = 16.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = WanderMuted, modifier = Modifier.size(13.dp))
    }
}
