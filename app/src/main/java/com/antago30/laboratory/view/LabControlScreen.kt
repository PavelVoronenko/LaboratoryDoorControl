package com.antago30.laboratory.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import android.widget.Toast

@Composable
fun LabControlScreen(
    modifier: Modifier = Modifier,
    viewModel: LabControlViewModel = viewModel()
) {
    val context = LocalContext.current
    val staffList by viewModel.staffList
    val functions by viewModel.functions

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val (topBar, staffPanel, functionsPanel, actionButton) = createRefs()

        TopBar(
            isBroadcasting = viewModel.isBroadcasting,
            onSettingsButtonClick = {
                // Например: открыть экран настроек
                // Или, если ты хочешь именно "toggleTopBarStatus" — вызови его без id
                //viewModel.toggleTopBarStatus() // ← без id, если это глобальный флаг
            },
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
                //Toast.makeText(context, "Статус изменён", Toast.LENGTH_SHORT).show()
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
                val currentFunction = functions.find { it.id == id }
                val wasEnabled = currentFunction?.isEnabled ?: false
                val newName = currentFunction?.label ?: "Функция"

                viewModel.toggleFunction(id)

                Toast.makeText(context, "«$newName»", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Открыть дверь", Toast.LENGTH_SHORT).show()
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

