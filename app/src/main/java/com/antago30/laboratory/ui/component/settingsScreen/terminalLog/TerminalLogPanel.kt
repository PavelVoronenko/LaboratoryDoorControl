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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary
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

    val seenIds = remember { mutableSetOf<String>() }
    
    // При первой загрузке (когда приходит пачка из 500 логов) 
    // помечаем их все как "уже виденные", чтобы не анимировать
    LaunchedEffect(logs.isNotEmpty()) {
        if (seenIds.isEmpty() && logs.isNotEmpty()) {
            logs.forEach { seenIds.add(it.id) }
        }
    }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 100
        }
    }

    LaunchedEffect(isAtBottom, isDragged) {
        if (isAtBottom && !isDragged) {
            isAutoScrollEnabled = true
        } else if (isDragged && !isAtBottom) {
            isAutoScrollEnabled = false
        }
    }

    // Плавный автоскролл при появлении нового лога (в начале списка)
    LaunchedEffect(logs.firstOrNull()?.id) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(isAutoScrollEnabled) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(screenHeight * 0.5f)
            .graphicsLayer { alpha = panelAlpha },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.15f)),
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
                        color = Color.Gray.copy(alpha = 0.5f),
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
                                // Если лог уже был в списке (история), рендерим его без анимации
                                val isNew = remember(entry.id) { !seenIds.contains(entry.id) }
                                
                                if (isNew) {
                                    val visibleState = remember(entry.id) {
                                        MutableTransitionState(false).apply {
                                            targetState = true
                                        }
                                    }
                                    
                                    LaunchedEffect(entry.id) {
                                        seenIds.add(entry.id)
                                    }

                                    this@Column.AnimatedVisibility(
                                        visibleState = visibleState,
                                        enter = expandVertically(
                                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                                            expandFrom = Alignment.Bottom
                                        ) + fadeIn(tween(400)),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        TerminalLogRow(entry)
                                    }
                                } else {
                                    // Исторический лог без анимаций и тяжелых оберток
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = entry.getFormattedTime(),
            color = entry.getTimeColor(),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = entry.message,
            color = entry.getMessageColor(),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
