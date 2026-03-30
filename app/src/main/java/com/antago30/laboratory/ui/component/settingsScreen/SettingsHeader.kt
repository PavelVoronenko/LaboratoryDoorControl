package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onBleDeviceClick: () -> Unit = {},
    onUserClick: () -> Unit = {},
    showBleButton: Boolean = true,
    showUserButton: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var backScale by remember { mutableFloatStateOf(1f) }
    var bleScale by remember { mutableFloatStateOf(1f) }
    var userScale by remember { mutableFloatStateOf(1f) }

    val animatedBackScale by animateFloatAsState(targetValue = backScale)
    val animatedBleScale by animateFloatAsState(targetValue = bleScale)
    val animatedUserScale by animateFloatAsState(targetValue = userScale)

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

        // === Правая часть: кнопки BLE и Пользователь ===
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
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Выбрать BLE-устройство",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Кнопка пользователя
            if (showUserButton) {
                IconButton(
                    onClick = {
                        userScale = 0.7f
                        scope.launch {
                            delay(150)
                            userScale = 1f
                        }
                        onUserClick()
                    },
                    modifier = Modifier
                        .scale(animatedUserScale)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Выбрать текущего пользователя",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}