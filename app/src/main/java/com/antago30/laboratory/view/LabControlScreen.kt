package com.antago30.laboratory.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antago30.laboratory.ui.component.FunctionsPanel
import com.antago30.laboratory.ui.component.OpenDoorButton
import com.antago30.laboratory.ui.component.StaffPanel
import com.antago30.laboratory.ui.component.TopBar
import com.antago30.laboratory.viewmodel.LabControlViewModel

@Composable
fun LabControlScreen(
    modifier: Modifier = Modifier,
    viewModel: LabControlViewModel = viewModel()
) {
    val context = LocalContext.current
    val staffList by viewModel.staffList
    val functions by viewModel.functions

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
            onSettingsButtonClick = { },
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
        )

        FunctionsPanel(
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
    }
}