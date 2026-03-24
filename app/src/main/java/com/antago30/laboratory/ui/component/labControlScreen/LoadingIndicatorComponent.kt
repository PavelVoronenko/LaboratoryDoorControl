package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicatorComponent(
    modifier: Modifier = Modifier,
    size: Float = 48f,
    ringColor: Color = Color(0xFF4A6FA5), // Сиреневый (залитый)
    ringPadding: Float = 1f // Отступ между кольцом и индикатором
) {
    // Бесконечная анимация прогресса
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    // Размер кольца = размер индикатора + отступы
    val ringSize = size + ringPadding

    Box(
        modifier = modifier.size(ringSize.dp),
        contentAlignment = Alignment.Center
    ) {
        // 🔵 ЗАЛИТЫЙ СИРЕНЕВЫЙ КРУГ (фон)
        Box(
            modifier = Modifier
                .size(ringSize.dp)
                .clip(CircleShape)
                .background(ringColor) // ✅ БЕЗ alpha — сплошной цвет!
        )

        // ✨ INDETERMINATE LOADING INDICATOR (по центру)
        LoadingIndicator(
            progress = { progress },
            modifier = Modifier.size(size.dp),
            color = Color(0xFF7EB3D8),
            polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
        )
    }
}