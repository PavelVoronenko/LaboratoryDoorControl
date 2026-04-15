package com.antago30.laboratory.ui.component.labControlScreen

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.R
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    isBroadcasting: Boolean = false,
    onSettingsButtonClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var settingsScale by remember { mutableFloatStateOf(1f) }
    val rotation = remember { Animatable(0f) }

    val animatedSettingsScale by animateFloatAsState(
        targetValue = settingsScale,
        label = "SettingsButtonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка вещания
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBroadcasting,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = radarScale
                                scaleY = radarScale
                                alpha = radarAlpha
                            }
                            .border(1.5.dp, Primary.copy(alpha = 0.7f), CircleShape)
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.advertise),
                        contentDescription = stringResource(R.string.startAdvertising),
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Кнопка настроек
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(animatedSettingsScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Primary, bounded = false),
                    onClick = {
                        settingsScale = 0.85f
                        scope.launch {
                            rotation.animateTo(
                                targetValue = rotation.value + 90f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        scope.launch {
                            delay(100)
                            settingsScale = 1f
                            onSettingsButtonClick()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.settings),
                tint = Primary.copy(alpha = 0.75f),
                modifier = Modifier 
                    .size(30.dp)
                    .rotate(rotation.value)
            )
        }
    }
}

