package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    showBleButton: Boolean = true,
    connectionState: ConnectionState = ConnectionState.DISCONNECTED
) {
    val scope = rememberCoroutineScope()
    var backScale by remember { mutableFloatStateOf(1f) }
    var bleScale by remember { mutableFloatStateOf(1f) }

    val animatedBackScale by animateFloatAsState(targetValue = backScale)
    val animatedBleScale by animateFloatAsState(targetValue = bleScale)

    val isConnected = connectionState == ConnectionState.READY

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // === Левая часть: кнопка назад ===
        IconButton(
            onClick = {
                backScale = 0.7f
                scope.launch {
                    delay(150)
                    backScale = 1f
                }
                onBack()
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .scale(animatedBackScale)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(32.dp)
            )
        }

        // === Центр: заголовок ===
        Text(
            text = "Настройки",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
        )

        // === Правая часть: кнопка BLE ===
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Кнопка BLE
            if (showBleButton) {
                IconButton(
                    onClick = {
                        bleScale = 0.7f
                        scope.launch {
                            delay(150)
                            bleScale = 1f
                        }
                        onBleDeviceClick()
                    },
                    modifier = Modifier
                        .scale(animatedBleScale)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = if (isConnected) "Подключено" else "Выбрать BLE-устройство",
                        tint = if (isConnected) Primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}