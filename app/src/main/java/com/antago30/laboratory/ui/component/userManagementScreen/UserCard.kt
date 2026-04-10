package com.antago30.laboratory.ui.component.userManagementScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserCard(
    user: UserInfo,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isConnected: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "cardScale")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBg.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
    ) {
        // Row: слева контент, справа кнопка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        scale = 0.96f
                        scope.launch {
                            delay(200)
                            scale = 1f
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ЛЕВАЯ ЧАСТЬ: аватар + текст
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Аватар
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D3748)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Имя
                    Text(
                        text = "${user.id}. ${user.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE8F0FE)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // MAC + UUID + SD в плотной группе
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp) // ✅ Минимальный отступ между строками
                    ) {
                        Text(
                            text = "MAC: ${user.macAddress}",
                            fontSize = 11.sp,
                            color = Primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 11.sp
                        )

                        Text(
                            text = "UUID: ${truncateUuid(user.uuid)} | SD: ${user.serviceData}",
                            fontSize = 10.sp,  // ✅ Чуть меньше шрифт
                            color = Color(0xFFA0AEC0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = isConnected,
                        onClick = onDeleteClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = if (isConnected) Color(0xFFF56565) else Color(0xFF6B7280),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun truncateUuid(uuid: String): String {
    return if (uuid.length > 12) {
        "${uuid.take(8)}…${uuid.takeLast(6)}"
    } else {
        uuid
    }
}