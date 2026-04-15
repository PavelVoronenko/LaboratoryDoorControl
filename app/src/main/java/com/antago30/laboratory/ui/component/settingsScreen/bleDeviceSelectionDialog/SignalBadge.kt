package com.antago30.laboratory.ui.component.settingsScreen.bleDeviceSelectionDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.theme.InLab
import com.antago30.laboratory.ui.theme.Outdoor
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.ui.theme.TextMuted

@Composable
fun SignalBadge(rssi: Int) {
    val signalColor = when {
        rssi > -60 -> InLab
        rssi > -80 -> Primary
        else -> Outdoor
    }

    val signalAlpha = when {
        rssi > -60 -> 1.0f
        rssi > -80 -> 0.7f
        else -> 0.4f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(signalColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SignalCellularAlt,
            contentDescription = null,
            tint = signalColor.copy(alpha = signalAlpha),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$rssi",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = TextMuted
        )
    }
}