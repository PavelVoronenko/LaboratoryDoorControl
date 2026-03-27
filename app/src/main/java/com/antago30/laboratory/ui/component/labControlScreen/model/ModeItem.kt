package com.antago30.laboratory.ui.component.labControlScreen.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.ui.theme.SwitchUnchecked
import com.antago30.laboratory.ui.theme.Text

@Composable
fun ModeItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val disabledTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) Text else disabledTextColor
        )

        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                // Активное состояние
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SwitchUnchecked,

                // Неактивное состояние (когда enabled = false)
                disabledCheckedThumbColor = disabledThumbColor,
                disabledCheckedTrackColor = disabledTrackColor,
                disabledUncheckedThumbColor = disabledThumbColor,
                disabledUncheckedTrackColor = disabledTrackColor,
                disabledCheckedIconColor = disabledThumbColor,
                disabledUncheckedIconColor = disabledThumbColor
            )
        )
    }
}