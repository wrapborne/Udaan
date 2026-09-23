package com.viplove.licadvisornative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.viplove.licadvisornative.ui.theme.BrandGoldSoft
import com.viplove.licadvisornative.ui.theme.BrandSuccess
import com.viplove.licadvisornative.ui.theme.BrandSuccessBg

enum class AvatarPalette {
    Blue,
    Gold,
    Green,
    Amber,
    Neutral
}

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    palette: AvatarPalette = AvatarPalette.Blue,
    size: Dp = 32.dp
) {
    val colors = when (palette) {
        AvatarPalette.Blue -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        AvatarPalette.Gold -> BrandGoldSoft to MaterialTheme.colorScheme.onSecondaryContainer
        AvatarPalette.Green -> BrandSuccessBg to BrandSuccess
        AvatarPalette.Amber -> Color(0xFFFEF3C7) to Color(0xFFF59E0B)
        AvatarPalette.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(size)
            .background(colors.first, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.second
        )
    }
}
