package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.antago30.laboratory.ui.theme.Primary

@Composable
fun UserManagementFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        containerColor = if (enabled) Primary else Color(0xFF6B7280),
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Добавить пользователя",
            tint = Color.White
        )
    }
}