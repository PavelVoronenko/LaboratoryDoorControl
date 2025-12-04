package com.antago30.laboratory.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.component.model.ModeItem
import com.antago30.laboratory.ui.theme.Primary
import kotlin.collections.forEach

@Composable
fun FunctionsPanel(
    functions: List<FunctionItem>,
    onFunctionToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            functions.forEach { item ->
                ModeItem(
                    label = item.label,
                    checked = item.isEnabled,
                    onCheckedChange = {
                        onFunctionToggled(item.id)
                    }
                )
            }
        }
    }
}