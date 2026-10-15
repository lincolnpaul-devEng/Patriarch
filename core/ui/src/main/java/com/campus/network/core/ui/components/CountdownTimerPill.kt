package com.campus.network.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.theme.CampusAlertRed
import com.campus.network.core.ui.theme.CampusPrimary
import kotlinx.coroutines.delay

/**
 * High-speed countdown timer pill for dynamic Flash-Meet map pins and cards.
 * Avoids parent recomposition by isolating local state updates.
 */
@Composable
fun CountdownTimerPill(
    expiresAtEpochMs: Long,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(expiresAtEpochMs) {
        mutableLongStateOf(((expiresAtEpochMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(expiresAtEpochMs) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds = ((expiresAtEpochMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        }
    }

    val isUrgent = remainingSeconds in 1..600 // Under 10 minutes
    val isExpired = remainingSeconds <= 0

    val backgroundColor = when {
        isExpired -> Color(0xFF4A4E57)
        isUrgent -> CampusAlertRed.copy(alpha = 0.15f)
        else -> CampusPrimary.copy(alpha = 0.15f)
    }

    val contentColor = when {
        isExpired -> Color(0xFF9E9E9E)
        isUrgent -> CampusAlertRed
        else -> CampusPrimary
    }

    val formattedTime = remember(remainingSeconds) {
        if (isExpired) {
            "Expired"
        } else {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            String.format("%02d:%02d left", minutes, seconds)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formattedTime,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
