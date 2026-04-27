package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementTopBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    title: String? = "Пользователи",
    showBackButton: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    var backScale by remember { mutableFloatStateOf(1f) }
    val animatedBackScale by animateFloatAsState(targetValue = backScale)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // === Левая часть: кнопка назад ===
        if (showBackButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .scale(animatedBackScale)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Primary, bounded = false),
                        onClick = {
                            backScale = 0.85f
                            scope.launch {
                                delay(100)
                                backScale = 1f
                                onBack()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                    tint = Primary.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // === Центр: заголовок ===
        title?.let {
            Text(
                text = it,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                letterSpacing = 0.5.sp
            )
        }
    }
}