package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.15f))
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
                .padding(vertical = 16.dp)
        ) {
            functions.forEach { item ->
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
            }
        }
    }
}