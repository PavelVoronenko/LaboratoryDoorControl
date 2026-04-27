package com.antago30.laboratory.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: SettingsScreenViewModel,
    onBack: () -> Unit,
    connectionManager: com.antago30.laboratory.ble.BleConnectionManager
) {
    var distanceText by remember { mutableStateOf("") }
    var doorTimeText by remember { mutableStateOf("") }
    var doorCooldownText by remember { mutableStateOf("") }

    // Состояния фокуса для скрытия placeholder
    var isDistanceFocused by remember { mutableStateOf(false) }
    var isDoorTimeFocused by remember { mutableStateOf(false) }
    var isDoorCooldownFocused by remember { mutableStateOf(false) }

    val realDistance by viewModel.debugDistance.collectAsState()
    val currentThreshold by viewModel.debugThreshold.collectAsState()
    val currentDoorTime by viewModel.debugDoorTime.collectAsState()
    val currentDoorCooldown by viewModel.debugDoorCooldown.collectAsState()
    val rtcTime by viewModel.debugRtcTime.collectAsState()
    val temperature by viewModel.debugTemperature.collectAsState()
    val isBatteryOk by viewModel.isBatteryOk.collectAsState()
    val bleConnectionState by connectionManager.connectionStateFlow.collectAsState()
    
    val isEnabled = bleConnectionState == com.antago30.laboratory.model.ConnectionState.READY

    val contentAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.4f,
        animationSpec = tween(500),
        label = "contentAlpha"
    )

    // Состояние для времени телефона
    var phoneTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    
    // Состояния для анимации синхронизации
    var isSyncing by remember { mutableStateOf(false) }
    val syncRotation by animateFloatAsState(
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "syncRotation",
        finishedListener = { isSyncing = false }
    )
    val syncIconColor by animateColorAsState(
        targetValue = if (isSyncing) Color(0xFF4CAF50) else Primary,
        animationSpec = tween(durationMillis = 400),
        label = "syncColor"
    )

    // Состояния для анимации порога
    var isThresholdUpdating by remember { mutableStateOf(false) }
    val thresholdRotation by animateFloatAsState(
        targetValue = if (isThresholdUpdating) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "thresholdRotation",
        finishedListener = { isThresholdUpdating = false }
    )
    val thresholdButtonColor by animateColorAsState(
        targetValue = when {
            isThresholdUpdating -> Color(0xFF4CAF50)
            isEnabled && distanceText.isNotBlank() -> Primary
            else -> Color(0xFF94A3B8) // Холодный серый вместо обычного серого
        },
        animationSpec = tween(durationMillis = 400),
        label = "thresholdButtonColor"
    )

    // Состояния для анимации параметров двери
    var isDoorUpdating by remember { mutableStateOf(false) }
    val doorRotation by animateFloatAsState(
        targetValue = if (isDoorUpdating) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "doorRotation",
        finishedListener = { isDoorUpdating = false }
    )
    val doorButtonColor by animateColorAsState(
        targetValue = when {
            isDoorUpdating -> Color(0xFF4CAF50)
            isEnabled && (doorTimeText.isNotBlank() || doorCooldownText.isNotBlank()) -> Primary
            else -> Color(0xFF94A3B8) // Холодный серый вместо обычного серого
        },
        animationSpec = tween(durationMillis = 400),
        label = "doorButtonColor"
    )

    // Состояние для анимации перезагрузки
    var isRebooting by remember { mutableStateOf(false) }
    val rebootRotation by animateFloatAsState(
        targetValue = if (isRebooting) 720f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "rebootRotation",
        finishedListener = { isRebooting = false }
    )

    // Обновление времени телефона каждую секунду
    LaunchedEffect(Unit) {
        while (true) {
            phoneTime = java.time.LocalTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(bleConnectionState) {
        if (bleConnectionState == com.antago30.laboratory.model.ConnectionState.READY) {
            viewModel.startDebugObservation()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDebugObservation()
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = "Debug",
                onBack = onBack,
                showBackButton = false,
                showBleButton = false,
                showJdeButton = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp), // Убрал vertical padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp) // Добавил небольшие отступы здесь
            ) {
                // RTC Time Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = contentAlpha },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.background(
                            Brush.verticalGradient(listOf(Primary.copy(alpha = 0.08f), Primary.copy(alpha = 0.02f)))
                        )) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, null, tint = Primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Real Time Clock", fontWeight = FontWeight.Bold, color = Primary, fontSize = 18.sp)
                                    }
                                    
                                    Text(
                                        text = phoneTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF94A3B8), // Тот же цвет, что и у кнопок в покое
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Controller Time", fontSize = 14.sp, color = Color(0xFF94A3B8))
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                color = Primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(22.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${temperature}°C",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Primary
                                                    )
                                                }
                                            }
                                            
                                            Spacer(Modifier.width(8.dp))
                                            
                                            Surface(
                                                color = (if (isBatteryOk) Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(22.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isBatteryOk) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (isBatteryOk) Color(0xFF4CAF50) else Color(0xFFF44336)
                                                    )
                                                }
                                            }
                                        }
                                        val parts = rtcTime.split(" ")
                                        if (parts.size == 2) {
                                            Text(
                                                text = parts[0],
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Primary.copy(alpha = 0.7f),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = parts[1],
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.offset(y = 4.dp)
                                            )
                                        } else {
                                            Text(
                                                text = rtcTime,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { 
                                            isSyncing = true
                                            viewModel.syncRtcTime() 
                                        },
                                        enabled = isEnabled,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(syncIconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync, 
                                            contentDescription = "Sync Time",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .graphicsLayer { rotationZ = syncRotation },
                                            tint = syncIconColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // HC-SR04 Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = contentAlpha },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.background(
                            Brush.verticalGradient(listOf(Primary.copy(alpha = 0.08f), Primary.copy(alpha = 0.02f)))
                        )) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sensors, null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("HC-SR04", fontWeight = FontWeight.Bold, color = Primary, fontSize = 18.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    InfoBox(
                                        label = "Distance",
                                        value = "%.1f".format(realDistance),
                                        unit = "cm",
                                        color = Primary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    val isTriggered = realDistance <= currentThreshold && realDistance > 0.1f
                                    val thresholdColor by animateColorAsState(
                                        targetValue = if (isTriggered) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                        label = "thresholdHighlight"
                                    )

                                    InfoBox(
                                        label = "Threshold",
                                        value = "$currentThreshold",
                                        unit = "cm",
                                        color = if (isTriggered) Color(0xFF4CAF50) else Color(0xFF94A3B8), // Используем холодный серый
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = distanceText,
                                        onValueChange = { if (it.length <= 3) distanceText = it.filter { char -> char.isDigit() } },
                                        placeholder = { if (!isDistanceFocused) Text("New threshold") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .onFocusChanged { 
                                                isDistanceFocused = it.isFocused
                                                if (it.isFocused) distanceText = "" 
                                            },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        enabled = isEnabled,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = Primary.copy(alpha = 0.2f),
                                            focusedPlaceholderColor = Color(0xFF94A3B8),
                                            unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val dist = distanceText.toIntOrNull()
                                            if (dist != null) {
                                                isThresholdUpdating = true
                                                viewModel.sendDistanceThreshold(dist)
                                                distanceText = ""
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                        },
                                        modifier = Modifier.height(56.dp).width(100.dp),
                                        enabled = (isEnabled && distanceText.isNotBlank()) || isThresholdUpdating,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = thresholdButtonColor.copy(alpha = 0.1f),
                                            contentColor = thresholdButtonColor,
                                            disabledContainerColor = thresholdButtonColor.copy(alpha = 0.1f),
                                            disabledContentColor = thresholdButtonColor
                                        )
                                    ) {
                                        if (isThresholdUpdating) {
                                            Icon(
                                                imageVector = Icons.Default.Done, 
                                                contentDescription = null,
                                                modifier = Modifier.graphicsLayer { rotationZ = thresholdRotation },
                                                tint = thresholdButtonColor
                                            )
                                        } else {
                                            Text("ОК", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Door Control Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = contentAlpha },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.background(
                            Brush.verticalGradient(listOf(Primary.copy(alpha = 0.08f), Primary.copy(alpha = 0.02f)))
                        )) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LockOpen, null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Door", fontWeight = FontWeight.Bold, color = Primary, fontSize = 18.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    InfoBox(
                                        label = "Open Time",
                                        value = "%.1f".format(currentDoorTime / 1000f),
                                        unit = "s",
                                        color = Primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    InfoBox(
                                        label = "Cooldown",
                                        value = "%.1f".format(currentDoorCooldown / 1000f),
                                        unit = "s",
                                        color = Primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = doorTimeText,
                                            onValueChange = { 
                                                if (it.length <= 5) {
                                                    val filtered = it.replace(',', '.')
                                                    if (filtered.count { char -> char == '.' } <= 1) {
                                                        doorTimeText = filtered.filter { char -> char.isDigit() || char == '.' }
                                                    }
                                                }
                                            },
                                            placeholder = { if (!isDoorTimeFocused) Text("Open (s)") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .onFocusChanged { 
                                                    isDoorTimeFocused = it.isFocused
                                                    if (it.isFocused) doorTimeText = "" 
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            enabled = isEnabled,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Primary,
                                                unfocusedBorderColor = Primary.copy(alpha = 0.2f),
                                                focusedPlaceholderColor = Color(0xFF94A3B8),
                                                unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                            )
                                        )
                                        OutlinedTextField(
                                            value = doorCooldownText,
                                            onValueChange = { 
                                                if (it.length <= 5) {
                                                    val filtered = it.replace(',', '.')
                                                    if (filtered.count { char -> char == '.' } <= 1) {
                                                        doorCooldownText = filtered.filter { char -> char.isDigit() || char == '.' }
                                                    }
                                                }
                                            },
                                            placeholder = { if (!isDoorCooldownFocused) Text("Pause (s)") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .onFocusChanged { 
                                                    isDoorCooldownFocused = it.isFocused
                                                    if (it.isFocused) doorCooldownText = "" 
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            enabled = isEnabled,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Primary,
                                                unfocusedBorderColor = Primary.copy(alpha = 0.2f),
                                                focusedPlaceholderColor = Color(0xFF94A3B8),
                                                unfocusedPlaceholderColor = Color(0xFF94A3B8)
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val time = doorTimeText.toFloatOrNull()?.let { (it * 1000).toInt() } ?: currentDoorTime
                                            val cooldown = doorCooldownText.toFloatOrNull()?.let { (it * 1000).toInt() } ?: currentDoorCooldown
                                            isDoorUpdating = true
                                            viewModel.sendDoorParams(time, cooldown)
                                            doorTimeText = ""
                                            doorCooldownText = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        enabled = (isEnabled && (doorTimeText.isNotBlank() || doorCooldownText.isNotBlank())) || isDoorUpdating,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = doorButtonColor.copy(alpha = 0.1f),
                                            contentColor = doorButtonColor,
                                            disabledContainerColor = doorButtonColor.copy(alpha = 0.1f),
                                            disabledContentColor = doorButtonColor
                                        )
                                    ) {
                                        if (isDoorUpdating) {
                                            Icon(
                                                imageVector = Icons.Default.DoneAll, 
                                                contentDescription = null,
                                                modifier = Modifier.graphicsLayer { rotationZ = doorRotation },
                                                tint = doorButtonColor
                                            )
                                        } else {
                                            Text("Update Door Params", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Static Bottom Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isEnabled) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Primary,
                        trackColor = Primary.copy(alpha = 0.1f),
                        strokeWidth = 5.dp
                    )
                }

                Surface(
                    onClick = { 
                        isRebooting = true
                        viewModel.rebootController() 
                    },
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rebootRotation }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "REBOOT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 15.sp, color = Color(0xFF94A3B8)) // Принудительно холодный серый
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 14.sp, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(start = 2.dp, bottom = 0.dp))
            }
        }
    }
}
