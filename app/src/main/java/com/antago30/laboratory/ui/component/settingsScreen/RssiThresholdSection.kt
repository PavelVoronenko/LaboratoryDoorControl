package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssiThresholdSection(
    modifier: Modifier = Modifier,
    initialEntry: String = "-60 дБм",
    initialExit: String = "-80 дБм",
    onThresholdsChanged: (entry: String, exit: String) -> Unit,
) {
    val rssiValues = (-100..-30 step 10).map { "$it дБм" }

    var entryThreshold by remember { mutableStateOf(initialEntry) }
    var exitThreshold by remember { mutableStateOf(initialExit) }
    var isEntryExpanded by remember { mutableStateOf(false) }
    var isExitExpanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(entryThreshold, exitThreshold) {
        onThresholdsChanged(entryThreshold, exitThreshold)
        onDispose { }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D35)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Пороги BLE-реакции",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Порог входа
            ExposedDropdownMenuBox(
                expanded = isEntryExpanded,
                onExpandedChange = { isEntryExpanded = it }
            ) {
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF1E1E25),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { isEntryExpanded = true }
                        .padding(12.dp)
                ) {
                    Text(
                        "Порог входа: $entryThreshold",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = isEntryExpanded,
                    onDismissRequest = { isEntryExpanded = false }
                ) {
                    rssiValues.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value, color = Color.White) },
                            onClick = {
                                entryThreshold = value
                                isEntryExpanded = false
                            }
                        )
                    }
                }
            }

            // Порог выхода
            ExposedDropdownMenuBox(
                expanded = isExitExpanded,
                onExpandedChange = { isExitExpanded = it }
            ) {
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF1E1E25),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { isExitExpanded = true }
                        .padding(12.dp)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        "Порог выхода: $exitThreshold",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = isExitExpanded,
                    onDismissRequest = { isExitExpanded = false }
                ) {
                    rssiValues.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(value, color = Color.White) },
                            onClick = {
                                exitThreshold = value
                                isExitExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}