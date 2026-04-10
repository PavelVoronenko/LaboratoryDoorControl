package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.model.NewUserParams
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.util.MacAddressVisualTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAddForm(
    modifier: Modifier = Modifier,
    userId: String,
    userName: String,
    selectedUuid: String,
    selectedServiceData: String,
    macAddress: String,
    rssiThreshold: String,
    isLoading: Boolean,
    isAddingUser: Boolean,
    onUserNameChange: (String) -> Unit,
    onMacAddressChange: (String) -> Unit,
    onRssiThresholdChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: (NewUserParams) -> Unit,
    onError: (String) -> Unit,
    getNextAvailableId: () -> Int,
    getNextAvailableUuid: () -> String,
    getNextAvailableServiceData: () -> String,
    areUuidsExhausted: Boolean,
    areServiceDataExhausted: Boolean,
    isConnected: Boolean = true,
) {
    val isLimitExhausted = areUuidsExhausted || areServiceDataExhausted
    val scope = rememberCoroutineScope()
    var buttonScale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = buttonScale,
        label = "addButtonScale"
    )

    // ✅ Корневой Box: всё пространство экрана
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // ✅ Скроллимая область с полями формы
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Поля формы
            SimpleReadOnlyField(value = userId, label = "ID", leadingIcon = Icons.Default.Numbers)

            SimpleTextField(
                value = userName,
                onValueChange = onUserNameChange,
                label = "Имя и Отчество",
                placeholder = "Введите имя",
                leadingIcon = Icons.Default.Person,
                labelFontSize = 18.sp,
                labelFontWeight = FontWeight.Medium,
                enabled = !isLimitExhausted
            )

            SimpleReadOnlyField(
                value = selectedUuid,
                label = "UUID",
                leadingIcon = Icons.Default.Link
            )
            SimpleReadOnlyField(
                value = selectedServiceData,
                label = "Service Data",
                leadingIcon = Icons.Default.Key
            )

            SimpleTextField(
                value = macAddress,
                onValueChange = onMacAddressChange,
                label = "MAC-адрес",
                placeholder = "AA:BB:CC:DD:EE:FF",
                leadingIcon = Icons.Default.Bluetooth,
                enabled = !isLimitExhausted,
                visualTransformation = MacAddressVisualTransformation()
            )

            SimpleReadOnlyField(
                value = rssiThreshold,
                label = "RSSI порог",
                leadingIcon = Icons.Default.SignalCellularAlt,
                trailingText = "dBm",
                trailingTextColor = Primary.copy(alpha = 0.6f)
            )

            // Заголовок или баннер лимита
            if (isLimitExhausted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Лимит достигнут",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF56565),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Максимальное количество пользователей — 10",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ✅ Фиксированная кнопка внизу — НЕ скроллится!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .scale(animatedScale),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isLimitExhausted && !isLoading && !isAddingUser && isConnected) {
                        Primary.copy(alpha = 0.15f)
                    } else {
                        CardBg.copy(alpha = 0.08f)
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (!isLimitExhausted && !isLoading && !isAddingUser && isConnected) {
                        Primary.copy(alpha = 0.4f)
                    } else {
                        Primary.copy(alpha = 0.15f)
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable(
                            enabled = !isLimitExhausted && !isLoading && !isAddingUser && isConnected
                        ) {
                            if (!isLimitExhausted && !isLoading && !isAddingUser) {
                                buttonScale = 0.96f
                                scope.launch {
                                    delay(150)
                                    buttonScale = 1f
                                }
                                val params = NewUserParams(
                                    id = userId.toIntOrNull() ?: getNextAvailableId(),
                                    name = userName,
                                    uuid = selectedUuid,
                                    serviceData = selectedServiceData,
                                    macAddress = macAddress,
                                    rssiThreshold = rssiThreshold.toIntOrNull() ?: -70
                                )
                                if (params.isValid()) {
                                    onAdd(params)
                                } else {
                                    onError("Проверьте данные")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading || isAddingUser) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Добавить пользователя",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isLimitExhausted && isConnected) Primary else Color(
                                0xFF6B7280
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ✅ Компонент: неактивное поле только для чтения
@Composable
private fun SimpleReadOnlyField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingText: String = "",
    trailingTextColor: Color = Color(0xFF6B7280),
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label, fontSize = 14.sp, color = Color(0xFF6B7280)) },
        readOnly = true,
        enabled = false,
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, null, tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = {
            if (trailingText.isNotEmpty()) {
                Text(
                    text = trailingText,
                    color = trailingTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color(0xFF374151),
            disabledTextColor = Color(0xFF9CA3AF),
            disabledLabelColor = Color(0xFF6B7280),
            disabledLeadingIconColor = Color(0xFF6B7280),
            disabledTrailingIconColor = Color(0xFF6B7280),
            disabledContainerColor = Color.Transparent
        )
    )
}

// ✅ Простое редактируемое поле с поддержкой enabled
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    suffix: String = "",
    labelFontSize: TextUnit = 14.sp,
    labelFontWeight: FontWeight = FontWeight.Normal,
    enabled: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        visualTransformation = visualTransformation,
        label = { Text(label, fontSize = labelFontSize, fontWeight = labelFontWeight) },
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(
                    placeholder,
                    color = if (enabled) Primary.copy(alpha = 0.5f) else Color(0xFF6B7280),
                    fontSize = 13.sp
                )
            }
        },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    icon, null,
                    tint = if (enabled) Primary.copy(alpha = 0.7f) else Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = {
            if (suffix.isNotEmpty()) {
                Text(
                    suffix,
                    color = if (enabled) Primary.copy(alpha = 0.6f) else Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (enabled) Primary.copy(alpha = 0.5f) else Color.Transparent,
            unfocusedBorderColor = if (enabled) Primary.copy(alpha = 0.2f) else Color(0xFF374151),
            cursorColor = Primary,
            disabledTextColor = Color(0xFF9CA3AF),
            disabledLabelColor = Color(0xFF6B7280),
            disabledLeadingIconColor = Color(0xFF6B7280),
            disabledTrailingIconColor = Color(0xFF6B7280),
            disabledContainerColor = Color.Transparent
        )
    )
}
