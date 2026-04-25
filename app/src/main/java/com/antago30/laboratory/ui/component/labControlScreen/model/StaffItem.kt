package com.antago30.laboratory.ui.component.labControlScreen.model

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.InLab
import com.antago30.laboratory.ui.theme.Outdoor
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.antago30.laboratory.ui.theme.Text as ThemeText

@Composable
fun StaffItem(
    member: StaffMember,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")

    val itemBackground = if (enabled) {
        Brush.verticalGradient(
            colors = listOf(
                CardBg.copy(alpha = 0.7f),
                CardBg.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                CardBg.copy(alpha = 0.35f),
                CardBg.copy(alpha = 0.15f)
            )
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.5.dp,
            if (enabled) Primary.copy(alpha = 0.15f) else Primary.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemBackground)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        scale = 0.96f
                        scope.launch {
                            delay(200)
                            scale = 1f
                        }
                        onClick()
                    }
                )
                .scale(animatedScale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .padding(8.dp)
            ) {
                // Градиентный фон аватара в зависимости от статуса
                val gradientColors = if (enabled) {
                    if (member.isInside) {
                        // Зелёный градиент (в лаборатории)
                        listOf(
                            InLab.copy(alpha = 0.4f),
                            InLab.copy(alpha = 0.12f)
                        )
                    } else {
                        // Красный градиент (на улице)
                        listOf(
                            Outdoor.copy(alpha = 0.4f),
                            Outdoor.copy(alpha = 0.12f)
                        )
                    }
                } else {
                    listOf(
                        Color.Gray.copy(alpha = 0.1f),
                        Color.Gray.copy(alpha = 0.03f)
                    )
                }

                // Цвет обводки аватара (статичный согласно теме)
                val borderColor = if (enabled) {
                    Primary.copy(alpha = 0.2f)
                } else {
                    Color.Gray.copy(alpha = 0.1f)
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.radialGradient(colors = gradientColors, center = Offset(0.5f, 0.5f), radius = 0.8f))
                        .then(Modifier.border(1.5.dp, borderColor, CircleShape))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.initials,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (enabled) ThemeText else Color.Gray.copy(alpha = 0.4f)
                    )
                }
            }

            Column {
                Text(
                    text = member.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color.Unspecified else Color.Gray.copy(alpha = 0.4f)
                )
                if (enabled) {
                    Text(
                        text = stringResource(
                            id = if (member.isInside) R.string.inside else R.string.outside
                        ),
                        fontSize = 14.sp,
                        color = if (member.isInside) InLab else Outdoor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}