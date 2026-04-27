package com.antago30.laboratory.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    val realDistance by viewModel.debugDistance.collectAsState()
    val currentThreshold by viewModel.debugThreshold.collectAsState()
    val currentDoorTime by viewModel.debugDoorTime.collectAsState()
    val currentDoorCooldown by viewModel.debugDoorCooldown.collectAsState()
    val bleConnectionState by connectionManager.connectionStateFlow.collectAsState()
    
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Запуск наблюдения при входе или ПЕРЕПОДКЛЮЧЕНИИ
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("HC-SR04", fontWeight = FontWeight.Bold, color = Primary, fontSize = 18.sp)
                        }

                        // Real-time Data Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoBox(
                                label = "Distance",
                                value = "%.1f".format(realDistance),
                                unit = "cm",
                                color = Primary,
                                modifier = Modifier.weight(1f)
                            )

                            val isTriggered = realDistance <= currentThreshold && realDistance > 0.1f
                            val thresholdColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isTriggered) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                                label = "thresholdHighlight"
                            )

                            InfoBox(
                                label = "Threshold",
                                value = "$currentThreshold",
                                unit = "cm",
                                color = thresholdColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Compact Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = distanceText,
                                onValueChange = { if (it.length <= 3) distanceText = it.filter { char -> char.isDigit() } },
                                placeholder = { Text("New threshold") },
                                modifier = Modifier.weight(1f).height(56.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Primary.copy(alpha = 0.2f),
                                    unfocusedPlaceholderColor = Primary.copy(alpha = 0.4f),
                                    focusedPlaceholderColor = Primary.copy(alpha = 0.4f)
                                )
                            )

                            Button(
                                onClick = {
                                    val dist = distanceText.toIntOrNull()
                                    if (dist != null) {
                                        viewModel.sendDistanceThreshold(dist)
                                        distanceText = ""
                                        keyboardController?.hide()
                                    }
                                },
                                modifier = Modifier.height(56.dp).width(100.dp),
                                enabled = distanceText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("ОК", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Door Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockOpen, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Door", fontWeight = FontWeight.Bold, color = Primary, fontSize = 18.sp)
                        }

                        // Current Values
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

                        // Inputs
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
                                    placeholder = { Text("Open (s)") },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Primary.copy(alpha = 0.2f)
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
                                    placeholder = { Text("Pause (s)") },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Primary.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    val time = doorTimeText.toFloatOrNull()?.let { (it * 1000).toInt() } ?: currentDoorTime
                                    val cooldown = doorCooldownText.toFloatOrNull()?.let { (it * 1000).toInt() } ?: currentDoorCooldown
                                    viewModel.sendDoorParams(time, cooldown)
                                    doorTimeText = ""
                                    doorCooldownText = ""
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = doorTimeText.isNotBlank() || doorCooldownText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update Door Params", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Futuristic Reboot Button
            Surface(
                onClick = { viewModel.rebootController() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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
                            modifier = Modifier.size(20.dp)
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
            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 14.sp, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(start = 2.dp, bottom = 0.dp))
            }
        }
    }
}
