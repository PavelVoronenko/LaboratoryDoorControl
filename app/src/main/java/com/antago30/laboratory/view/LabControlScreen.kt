package com.antago30.laboratory.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.antago30.laboratory.ui.component.labControlScreen.FunctionsPanel
import com.antago30.laboratory.ui.component.labControlScreen.LoadingIndicatorComponent
import com.antago30.laboratory.ui.component.labControlScreen.OpenDoorButton
import com.antago30.laboratory.ui.component.labControlScreen.StaffPanel
import com.antago30.laboratory.ui.component.labControlScreen.TopBar
import com.antago30.laboratory.viewmodel.LabControlViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            // Разрешение не получено
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

        if (isEnabled) {
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
            )
        } else {
            LoadingIndicatorComponent(
                modifier = Modifier.constrainAs(staffPanel) {
                    top.linkTo(topBar.bottom, margin = 4.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
                size = 256f
            )
        }

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
            enabled = isEnabled,
            modifier = Modifier.constrainAs(actionButton) {
                bottom.linkTo(parent.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )
    }
}