package com.antago30.laboratory.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.antago30.laboratory.ui.component.labControlScreen.FunctionsPanel
import com.antago30.laboratory.ui.component.labControlScreen.OpenDoorButton
import com.antago30.laboratory.ui.component.labControlScreen.StaffPanel
import com.antago30.laboratory.ui.component.labControlScreen.TopBar
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.util.ControllerMessageBanner
import com.antago30.laboratory.viewmodel.labControlViewModel.LabControlViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabControlScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    viewModel: LabControlViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEnabled by viewModel.isInterfaceEnabled.collectAsState()

    val staffList by viewModel.staffList.collectAsState()
    val functions by viewModel.functions.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val showBatteryWarning by viewModel.showBatteryWarning.collectAsState()

    // Диалог предупреждения о батарейке
    if (showBatteryWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBatteryWarning() },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF44336).copy(alpha = 0.12f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Батарея разряжена",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Рекомендуется заменить элемент CR2032 для сохранения точности логирования",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = { viewModel.dismissBatteryWarning() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = "ПОНЯТНО",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        )
    }

    // Лаунчер для запроса BLUETOOTH_ADVERTISE
    val advertisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startBleAdvertising()
        }
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val (topBar, staffPanel, functionsPanel, loadingIndicator, messageBanner, openDoorButton) = createRefs()

        TopBar(
            isBroadcasting = isAdvertising,
            onSettingsButtonClick = onSettingsClick,
            modifier = Modifier.constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        @Suppress("MissingPermission")
        StaffPanel(
            staffList = staffList,
            enabled = isEnabled,
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
            onFunctionToggled = { id, newState ->
                if (id == "broadcast") {
                    if (newState) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_ADVERTISE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startBleAdvertising()
                        } else {
                            advertisePermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
                        }
                    } else {
                        // Пользователь выключил → останавливаем рекламу
                        viewModel.stopBleAdvertising()
                        viewModel.toggleFunction(id)  // Обновляем тумблер
                    }
                } else {
                    viewModel.toggleFunction(id)
                }
            },
            isConnectionEnabled = isEnabled,
            modifier = Modifier.constrainAs(functionsPanel) {
                top.linkTo(staffPanel.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        if (!isEnabled) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .constrainAs(loadingIndicator) {
                        bottom.linkTo(openDoorButton.top, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                color = Primary,
                trackColor = Primary.copy(alpha = 0.1f),
                strokeWidth = 5.dp
            )
        }

        OpenDoorButton(
            onClick = {
                viewModel.onOpenDoorClicked()
            },
            enabled = isEnabled,
            modifier = Modifier.constrainAs(openDoorButton) {
                bottom.linkTo(parent.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        )

        // Баннер с сообщениями от контроллера
        val controllerMessage by viewModel.controllerToastMessage.collectAsState()

        ControllerMessageBanner(
            message = controllerMessage,
            onDismiss = { viewModel.clearControllerToastMessage() },
            modifier = Modifier.constrainAs(messageBanner) {
                bottom.linkTo(openDoorButton.top, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
    }
}