package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OpenDoorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled) scale else 0.98f,
        label = "openDoorScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .scale(animatedScale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.5.dp,
            if (enabled) Primary.copy(alpha = 0.25f) else Primary.copy(alpha = 0.08f)
        )
    ) {
        val buttonBackground = if (enabled) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(buttonBackground)
                .clickable(enabled = enabled) {
                    if (enabled) {
                        scale = 0.96f
                        scope.launch {
                            delay(150)
                            scale = 1f
                        }
                        onClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.openDoor),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Primary else Color.Gray.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}