package com.antago30.laboratory.ui.component.settingsScreen.terminalLog

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val scope = rememberCoroutineScope()

    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    
    val reversedLogs by remember(logs) {
        derivedStateOf { logs.asReversed() }
    }
    
    val seenIds = remember { mutableSetOf<String>() }
    
    LaunchedEffect(logs.isEmpty()) {
        if (logs.isEmpty()) {
            seenIds.clear()
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

    // Плавный автоскролл
    LaunchedEffect(logs.lastOrNull()?.id) {
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
            .height(screenHeight * 0.5f),
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
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isAutoScrollEnabled = !isAutoScrollEnabled
                            if (isAutoScrollEnabled && logs.isNotEmpty()) {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        },
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Очистить",
                            tint = Primary.copy(alpha = 0.6f),
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
                                items = reversedLogs,
                                key = { it.id }
                            ) { entry ->
                                val isAlreadySeen = remember(entry.id) { seenIds.contains(entry.id) }
                                
                                val visibleState = remember(entry.id) {
                                    MutableTransitionState(isAlreadySeen).apply {
                                        targetState = true
                                    }
                                }
                                
                                LaunchedEffect(entry.id) {
                                    seenIds.add(entry.id)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            ),
                                            fadeInSpec = null,
                                            fadeOutSpec = null
                                        )
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visibleState = visibleState,
                                        enter = expandVertically(
                                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                                            expandFrom = Alignment.Bottom
                                        ) + fadeIn(tween(400)),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "[${entry.getFormattedTime()}]", 
                                                modifier = Modifier.width(64.dp),
                                                color = Primary.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = entry.message,
                                                color = Color(0xFFE8F0FE),
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
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
