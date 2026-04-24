package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssiThresholdSection(
    modifier: Modifier = Modifier,
    initialEntry: String = "-60 дБм",
    initialExit: String = "-80 дБм",
    onThresholdsChanged: (entry: String, exit: String) -> Unit,
    isEnabled: Boolean = true
) {
    val rssiValues = (-100..-30 step 5).map { "$it дБм" }

    var entryThreshold by remember(initialEntry) { mutableStateOf(initialEntry) }
    var exitThreshold by remember(initialExit) { mutableStateOf(initialExit) }

    val sectionAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        label = "sectionAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = sectionAlpha },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ThresholdDropdown(
                label = "Порог для входа",
                value = entryThreshold,
                icon = Icons.AutoMirrored.Filled.Login,
                iconColor = Color(0xFF68D391),
                options = rssiValues,
                isEnabled = isEnabled,
                onSelected = { 
                    entryThreshold = it 
                    onThresholdsChanged(it, exitThreshold)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 4.dp),
                thickness = 0.5.dp,
                color = Primary.copy(alpha = 0.1f)
            )

            ThresholdDropdown(
                label = "Порог для выхода",
                value = exitThreshold,
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = Color(0xFFF56565),
                options = rssiValues,
                isEnabled = isEnabled,
                onSelected = { 
                    exitThreshold = it 
                    onThresholdsChanged(entryThreshold, it)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdDropdown(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    options: List<String>,
    isEnabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var containerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Авто-скролл к текущему значению при открытии
    LaunchedEffect(expanded, containerHeightPx) {
        if (expanded && containerHeightPx > 0) {
            val index = options.indexOf(value)
            if (index >= 0) {
                // Высота элемента (48dp padding + 8dp vertical gaps = ~52dp)
                val itemHeightPx = with(density) { 52.dp.toPx() }
                val targetScroll = (index * itemHeightPx - containerHeightPx / 2 + itemHeightPx / 2).toInt()
                scrollState.scrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
            }
        }
    }

    Box {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp, horizontal = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF70A0F1).copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .width(125.dp) // Увеличил ширину, чтобы "-100 дБм" точно влезало в одну строку
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEnabled) Primary.copy(alpha = 0.05f) 
                            else Color.Gray.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isEnabled) Primary.copy(alpha = 0.25f) 
                                    else Primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = isEnabled) { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isEnabled) Primary else Color.Gray.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1, // Запрет переноса
                        softWrap = false
                    )
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.ArrowDropUp
                        else
                            Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isEnabled) Primary.copy(alpha = 0.8f) 
                               else Color.Gray.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(surface = CardBg),
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 0.dp),
                    properties = PopupProperties(focusable = true),
                    modifier = Modifier
                        .width(125.dp)
                        .heightIn(max = 280.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(CardBg, Color(0xFF1A1F2B))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = Primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .onSizeChanged { containerHeightPx = it.height }
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(vertical = 8.dp)
                        ) {
                            options.forEach { option ->
                                val isSelected = option == value
                            DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if (isSelected) Primary else Color.White.copy(
                                                alpha = 0.9f
                                            ),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center, // Текст по центру
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    onClick = {
                                        onSelected(option)
                                        expanded = false
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Primary.copy(alpha = 0.1f) else Color.Transparent
                                        ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 0.dp, // Убираем лишние отступы для центрирования
                                        vertical = 12.dp
                                    )
                                )
                            }
                        }

                        // Кастомный скроллбар справа
                        if (containerHeightPx > 0 && scrollState.maxValue > 0) {
                            val scrollProgress by remember {
                                derivedStateOf {
                                    scrollState.value.toFloat() / scrollState.maxValue
                                }
                            }

                            val scrollbarAlpha by animateFloatAsState(
                                targetValue = if (scrollState.isScrollInProgress) 0.6f else 0.15f,
                                label = "alpha"
                            )

                            val thumbHeight = 40.dp
                            val containerHeight = with(density) { containerHeightPx.toDp() }
                            val availableHeight = containerHeight - thumbHeight

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd) // Справа
                                    .padding(end = 4.dp) // Уменьшаем отступ, чтобы скроллбар был ближе к краю
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .graphicsLayer { alpha = scrollbarAlpha }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(thumbHeight)
                                        .offset {
                                            IntOffset(
                                                0,
                                                (availableHeight.toPx() * scrollProgress).toInt()
                                            )
                                        }
                                        .background(Primary, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
