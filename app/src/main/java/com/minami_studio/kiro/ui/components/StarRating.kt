package com.minami_studio.kiro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minami_studio.kiro.ui.theme.WanderAccent
import com.minami_studio.kiro.ui.theme.WanderBlush

@Composable
fun StarRating(
    rating: Int,
    onRatingChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Int = 20
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Star $i",
                tint = if (i <= rating) WanderAccent else WanderBlush,
                modifier = Modifier
                    .size(size.dp)
                    .then(
                        if (onRatingChanged != null) Modifier.clickable { onRatingChanged(i) }
                        else Modifier
                    )
            )
        }
    }
}
