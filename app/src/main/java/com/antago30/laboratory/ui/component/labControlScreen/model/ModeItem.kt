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
import com.antago30.laboratory.ui.theme.Text

@Composable
fun ModeItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

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
                checkedTrackColor = Primary.copy(alpha = 0.45f),
                checkedBorderColor = Primary.copy(alpha = 0.7f),
                
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Primary.copy(alpha = 0.2f),

                // Неактивное состояние (когда enabled = false)
                disabledCheckedThumbColor = Color.Gray.copy(alpha = 0.3f),
                disabledCheckedTrackColor = Color.Gray.copy(alpha = 0.1f),
                disabledUncheckedThumbColor = Color.Gray.copy(alpha = 0.3f),
                disabledUncheckedTrackColor = Color.Transparent,
                disabledCheckedBorderColor = Color.Gray.copy(alpha = 0.2f),
                disabledUncheckedBorderColor = Color.Gray.copy(alpha = 0.1f)
            )
        )
    }
}