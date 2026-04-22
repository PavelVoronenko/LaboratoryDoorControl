package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onBleDeviceClick: () -> Unit = {},
    onReconnectJde: () -> Unit = {},
    showBleButton: Boolean = true,
    connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    isJdeConnected: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var backScale by remember { mutableFloatStateOf(1f) }
    var bleScale by remember { mutableFloatStateOf(1f) }
    var refreshScale by remember { mutableFloatStateOf(1f) }

    val animatedBackScale by animateFloatAsState(targetValue = backScale, label = "")
    val animatedBleScale by animateFloatAsState(targetValue = bleScale, label = "")
    val animatedRefreshScale by animateFloatAsState(targetValue = refreshScale, label = "")

    val isConnected = connectionState == ConnectionState.READY

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // === Левая часть: кнопка назад ===
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .scale(animatedBackScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Primary, bounded = false),
                    onClick = {
                        backScale = 0.85f
                        scope.launch {
                            delay(100)
                            backScale = 1f
                            onBack()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = Primary.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp)
            )
        }

        // === Центр: заголовок ===
        Text(
            text = "Настройки",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            letterSpacing = 0.5.sp
        )

        // === Правая часть: кнопки ===
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Кнопка переподключения освещения
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (isConnected && !isJdeConnected) animatedRefreshScale else 1f)
                    .clip(CircleShape)
                    .clickable(
                        enabled = isConnected && !isJdeConnected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Primary, bounded = false),
                        onClick = {
                            refreshScale = 0.85f
                            scope.launch {
                                delay(100)
                                refreshScale = 1f
                                onReconnectJde()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Переподключить освещение",
                    tint = when {
                        !isConnected -> Primary.copy(alpha = 0.25f)
                        isJdeConnected -> Primary // Зеленый если подключено
                        else -> Color(0xFFF56565) // Красный если не подключено
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            // Кнопка BLE
            if (showBleButton) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(animatedBleScale)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Primary, bounded = false),
                            onClick = {
                                bleScale = 0.85f
                                scope.launch {
                                    delay(100)
                                    bleScale = 1f
                                    onBleDeviceClick()
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = if (isConnected) "Подключено" else "Выбрать BLE-устройство",
                        tint = if (isConnected) Primary else Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
