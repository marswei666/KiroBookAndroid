package com.minami_studio.kiro.ui.subscription

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minami_studio.kiro.data.subscription.SubscriptionTier
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.Strings

@Composable
fun SubscriptionUpgradeDialog(
    currentEntryCount: Int,
    requiredTier: SubscriptionTier,
    strings: Strings,
    onUpgrade: (SubscriptionTier) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(WanderWarm)) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(WanderBlush).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, strings.close, tint = WanderInk, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Icon(Icons.Default.Star, null, tint = WanderAccent, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(strings.subUpgradeTitle, fontSize = 24.sp, fontFamily = FontFamily.Serif, color = WanderInk)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.subUpgradeDesc, fontSize = 14.sp, color = WanderMuted, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = WanderBlush.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(strings.subCurrentPlan, fontSize = 12.sp, color = WanderMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(strings.subFreePlan, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = WanderInk)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(strings.subEntriesUsed(currentEntryCount, SubscriptionTier.FREE.maxEntries), fontSize = 13.sp, color = WanderMuted)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                SubscriptionTier.entries.filter { it != SubscriptionTier.FREE }.forEach { tier ->
                    TierCard(tier = tier, isRecommended = tier == requiredTier, strings = strings, currentEntryCount = currentEntryCount, onClick = { onUpgrade(tier) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(48.dp))
                Text(strings.subRestore, fontSize = 14.sp, color = WanderAccent, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().clickable { }, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TierCard(tier: SubscriptionTier, isRecommended: Boolean, strings: Strings, currentEntryCount: Int = 0, onClick: () -> Unit) {
    val bgColor = if (isRecommended) WanderInk else Color.White
    val textColor = if (isRecommended) WanderCream else WanderInk
    val priceColor = if (isRecommended) WanderCream.copy(alpha = 0.8f) else WanderMuted

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommended) 8.dp else 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$${tier.priceUsd.toInt()}${strings.subPriceMonthly}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(WanderAccent).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(strings.subAutoUpgrade, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val maxText = if (tier.maxEntries == Int.MAX_VALUE) strings.subEntriesUsed(currentEntryCount, Int.MAX_VALUE) else strings.subEntriesUsed(currentEntryCount, tier.maxEntries)
                Text(maxText, fontSize = 12.sp, color = priceColor)
            }
            Icon(Icons.Default.ArrowForward, null, tint = if (isRecommended) WanderAccent else WanderMuted, modifier = Modifier.size(20.dp))
        }
    }
}
