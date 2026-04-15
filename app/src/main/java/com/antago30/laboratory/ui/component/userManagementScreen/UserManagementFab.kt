package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.theme.Primary

@Composable
fun UserManagementFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    val contentColor = if (enabled) Primary else Color.Gray.copy(alpha = 0.4f)
    val borderColor = if (enabled) Primary.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.2f)
    
    val backgroundBrush = if (enabled) {
        Brush.verticalGradient(
            colors = listOf(
                Primary.copy(alpha = 0.12f),
                Primary.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.1f),
                Color.Gray.copy(alpha = 0.03f)
            )
        )
    }

    Surface(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Primary),
                onClick = onClick
            ),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добавить пользователя",
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}