package com.antago30.laboratory.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.antago30.laboratory.ui.component.labControlScreen.FunctionsPanel
import com.antago30.laboratory.ui.component.labControlScreen.OpenDoorButton
import com.antago30.laboratory.ui.component.labControlScreen.StaffPanel
import com.antago30.laboratory.ui.component.labControlScreen.TopBar
import com.antago30.laboratory.viewmodel.LabControlViewModel

@Composable
fun LabControlScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    viewModel: LabControlViewModel,
) {
    val context = LocalContext.current
    val staffList by viewModel.staffList
    val functions by viewModel.functions
    val isEnabled by viewModel.isInterfaceEnabled.collectAsState()

    // Лаунчер для запроса BLUETOOTH_ADVERTISE
    val advertisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startBleAdvertising()
        } else {
            // Разрешение не получено - можно показать сообщение
            // viewModel._uiEvents.emit(UiEvent.showError("BLUETOOTH_ADVERTISE permission denied"))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setAppContext(context)
        viewModel.syncServiceState()
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .alpha(if (isEnabled) 1f else 0.5f) // Затемнение до 50%
            .clickable(enabled = !isEnabled) { } // Блокировка кликов по всему экрану
    ) {
        val (topBar, staffPanel, functionsPanel, actionButton) = createRefs()

        TopBar(
            isBroadcasting = viewModel.isAdvertising,
            onSettingsButtonClick = onSettingsClick,
            modifier = Modifier.constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        StaffPanel(
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
                .pointerInput(isEnabled) {
                    if (!isEnabled) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent()
                        }
                    }
                }
        )

        FunctionsPanel(
            functions = functions,
            onFunctionToggled = { id ->
                if (id == "broadcast") {
                    val wasEnabled = functions.find { it.id == "broadcast" }?.isEnabled == true
                    val nowEnabled = !wasEnabled

                    viewModel.toggleFunction(id)

                    if (nowEnabled) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_ADVERTISE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startBleAdvertising()
                        } else {
                            advertisePermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                        }
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
        )

        OpenDoorButton(
            onClick = {
                viewModel.onOpenDoorClicked()
            },
            modifier = Modifier.constrainAs(actionButton) {
                bottom.linkTo(parent.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        if (!isEnabled) {
            val (statusText) = createRefs()

            Text(
                text = "Ожидание подключения...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.constrainAs(statusText) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(actionButton.top, margin = 8.dp)
                }
            )
        }
    }
}