package com.antago30.laboratory.ui.component.settingsScreen.terminalLog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TerminalLogPanel(
    logs: List<TerminalLogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
    isTerminalActive: Boolean = false,
    isEnabled: Boolean = true
) {
    val listState = rememberLazyListState()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val scope = rememberCoroutineScope()

    val panelAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        animationSpec = tween(500),
        label = "panelAlpha"
    )

    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    val isAtBottom by remember {
        derivedStateOf {
            // В reverseLayout = true индекс 0 — это низ списка.
            // Если мы видим индекс 0 с минимальным оффсетом, значит мы в самом низу.
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 50
        }
    }

    // Управление состоянием автоскролла
    LaunchedEffect(isAtBottom, isDragged) {
        if (isAtBottom) {
            isAutoScrollEnabled = true
        } else if (isDragged) {
            isAutoScrollEnabled = false
        }
    }

    // Автоскролл к самому свежему логу (индекс 0 в reverseLayout)
    LaunchedEffect(logs.firstOrNull()?.id, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            // Небольшая задержка гарантирует, что LazyColumn уже знает о новом элементе 
            // и его размерах перед началом анимации скролла.
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(screenHeight * 0.5f)
            .graphicsLayer { alpha = panelAlpha },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isEnabled) Primary.copy(alpha = 0.15f) else Primary.copy(alpha = 0.05f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardBg.copy(alpha = 0.7f),
                            CardBg.copy(alpha = 0.35f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Терминал",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Индикатор приёма данных
                    val indicatorColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isTerminalActive && isEnabled) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f),
                        animationSpec = tween(durationMillis = 100),
                        label = "indicatorColor"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(indicatorColor, CircleShape)
                            .then(
                                if (isTerminalActive && isEnabled) {
                                    Modifier.background(
                                        Color(0xFF4CAF50).copy(alpha = 0.3f),
                                        CircleShape
                                    ).padding(2.dp)
                                } else Modifier
                            )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isEnabled) {
                                isAutoScrollEnabled = !isAutoScrollEnabled
                                if (isAutoScrollEnabled && logs.isNotEmpty()) {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowDown,
                            contentDescription = "Автоскролл",
                            tint = if (isAutoScrollEnabled) Primary else Primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onClearLogs,
                        enabled = isEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Очистить",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp,
                color = Primary.copy(alpha = 0.15f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Ожидание данных...",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 8.dp),
                            reverseLayout = true,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            items(
                                items = logs,
                                key = { it.id }
                            ) { entry ->
                                Box(modifier = Modifier.animateItem()) {
                                    TerminalLogRow(entry)
                                }
                            }
                        }

                        val scrollProgress by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val totalItems = layoutInfo.totalItemsCount
                                val visibleItemsCount = layoutInfo.visibleItemsInfo.size
                                if (totalItems <= visibleItemsCount || visibleItemsCount == 0) 0f
                                else {
                                    val maxScrollIndex = (totalItems - visibleItemsCount).coerceAtLeast(1)
                                    1f - (listState.firstVisibleItemIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                                }
                            }
                        }

                        val scrollbarAlpha by animateFloatAsState(
                            targetValue = if (isDragged || !isAtBottom) 0.6f else 0.15f,
                            animationSpec = tween(500),
                            label = "scrollbarAlpha"
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(2.dp)
                        ) {
                            val thumbHeight = 40.dp
                            val availableHeight = maxHeight - thumbHeight
                            
                            val animatedOffset by animateDpAsState(
                                targetValue = availableHeight * scrollProgress,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "thumbOffset"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(thumbHeight)
                                    .offset { IntOffset(0, animatedOffset.roundToPx()) }
                                    .background(
                                        Primary.copy(alpha = scrollbarAlpha),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogRow(entry: TerminalLogEntry) {
    if (entry.type == LogType.DATE_HEADER) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.message,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    } else {
        // Вычисляем ширину временной метки (10 символов [HH:mm:ss]) на основе размера шрифта.
        // Это позволяет выровнять сообщения в одну колонку, сохраняя поддержку масштабирования текста.
        val density = LocalDensity.current
        val timeWidth = with(density) { (13.sp * 6.05f).toDp() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.getFormattedTime(),
                modifier = Modifier.width(timeWidth),
                style = TextStyle(
                    color = entry.getTimeColor(),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                softWrap = false
            )

            Text(
                text = entry.message,
                color = entry.getMessageColor(),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
