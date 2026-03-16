package com.antago30.laboratory.view

import android.R.attr.bottom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.antago30.laboratory.ui.component.labControlScreen.FunctionsPanel
import com.antago30.laboratory.ui.component.labControlScreen.OpenDoorButton
import com.antago30.laboratory.ui.component.labControlScreen.StaffPanel
import com.antago30.laboratory.ui.component.labControlScreen.TopBar
import com.antago30.laboratory.ui.component.settingsScreen.LogItem
import com.antago30.laboratory.ui.component.settingsScreen.RssiThresholdSection
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.ui.component.settingsScreen.UserSection
import com.antago30.laboratory.ui.component.settingsScreen.model.LogEntry

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mockLogs = remember {
        val now = System.currentTimeMillis()
        listOf(
            LogEntry(now - 120_000, "SENSOR", "Дверь открыта"),
            LogEntry(now - 180_000, "BLE", "Вячеслав Олегович входит в лабораторию"),
            LogEntry(now - 240_000, "SYSTEM", "Освещение включено"),
            LogEntry(now - 300_000, "SYSTEM", "Дверь закрыта"),
            LogEntry(now - 360_000, "BLE", "Павел Евгеньевич покидает лабораторию"),
            LogEntry(now - 420_000, "BLE", "Владимир Викторович входит в лабораторию"),
            LogEntry(now - 480_000, "SYSTEM", "Освещение отключено"),
        )
    }

    val staffNames = listOf("Владимир Викторович", "Вячеслав Олегович", "Павел Евгеньевич")

    var entryThreshold by remember { mutableStateOf("-60 дБм") }
    var exitThreshold by remember { mutableStateOf("-80 дБм") }


    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        val (settingsHeader, logItem, functionsPanel, actionButton) = createRefs()

        SettingsHeader(
            onBack = onBack,
            modifier = Modifier.constrainAs(settingsHeader) {

            }
        )

        LazyColumn(
            modifier = Modifier.constrainAs(logItem) {
                top.linkTo(settingsHeader.bottom, margin = 4.dp)
                /*start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints*/
            }
                .fillMaxSize()
                .padding(horizontal = 8.dp)
            .padding(bottom = 400.dp), // ← оставить место под кнопку (см. шаг 2)
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            mockLogs.forEach { it ->
                item {
                    LogItem(log = it, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }


        /*StaffPanel(
            staffList = staffList,
            onStaffClicked = { id ->
                viewModel.toggleStaffStatus(id)
            },
            modifier = Modifier.constrainAs(staffPanel) {
                top.linkTo(topBar.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )*/

        /*FunctionsPanel(
            functions = functions,
            onFunctionToggled = { id ->
                if (id == "broadcast") {
                    val wasEnabled = functions.find { it.id == "broadcast" }?.isEnabled == true
                    val nowEnabled = !wasEnabled

                    viewModel.toggleFunction(id)

                    if (nowEnabled) {
                        viewModel.startBleAdvertising()
                    } else {
                        viewModel.stopBleAdvertising()
                    }
                } else {
                    viewModel.toggleFunction(id)
                }
            },
            modifier = Modifier.constrainAs(functionsPanel) {
                top.linkTo(staffPanel.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )*/

        /*OpenDoorButton(
            onClick = {
                viewModel.onOpenDoorClicked()
            },
            modifier = Modifier.constrainAs(actionButton) {
                bottom.linkTo(parent.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )*/
    }


    /*LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
            //.padding(bottom = 80.dp), // ← оставить место под кнопку (см. шаг 2)
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SettingsHeader(onBack = onBack) // ← TopAppBar как обычный элемент списка!
        }

        item {
            UserSection(
                staffNames = staffNames,
                onAddUser = { name -> }
            )
        }

        item {
            RssiThresholdSection(
                initialEntry = entryThreshold,
                initialExit = exitThreshold,
                onThresholdsChanged = { entry, exit ->
                    entryThreshold = entry
                    exitThreshold = exit
                }
            )
        }

        item {
            androidx.compose.material3.Text(
                text = "Журнал событий",
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(mockLogs) { log ->
            LogItem(log = log, modifier = Modifier.padding(vertical = 4.dp))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = { /* TODO */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DBAFF)),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Text(
                "Сохранить настройки",
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 18.sp
            )
        }
    }*/
}