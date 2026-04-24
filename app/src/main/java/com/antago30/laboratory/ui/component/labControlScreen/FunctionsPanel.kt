package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.ui.component.labControlScreen.model.ModeItem
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary

@Composable
fun FunctionsPanel(
    functions: List<FunctionItem>,
    onFunctionToggled: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isConnectionEnabled: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.5.dp,
            if (isConnectionEnabled) Primary.copy(alpha = 0.25f) else Primary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardBg.copy(alpha = 0.7f),
                            CardBg.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(vertical = 12.dp)
        ) {
            functions.forEachIndexed { index, item ->
                val isToggleEnabled = if (item.requiresConnection) isConnectionEnabled else true
                val actualChecked = item.isEnabled

                ModeItem(
                    label = item.label,
                    checked = actualChecked,
                    onCheckedChange = { newState ->
                        onFunctionToggled(item.id, newState)
                    },
                    enabled = isToggleEnabled
                )

                if (index < functions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = Primary.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}