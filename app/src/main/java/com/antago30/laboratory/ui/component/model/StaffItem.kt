package com.antago30.laboratory.ui.component.model

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.ui.theme.CardBg
import com.antago30.laboratory.ui.theme.InLab
import com.antago30.laboratory.ui.theme.Outdoor
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.ui.theme.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StaffItem(
    member: StaffMember,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scale = 0.96f
                    scope.launch {
                        delay(200)
                        scale = 1f
                    }
                    onClick()
                }
                .scale(animatedScale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier
                .size(72.dp)
                .padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (member.isInside) InLab.copy(alpha = 0.3f) else Outdoor.copy(
                                alpha = 0.3f
                            ),
                            shape = CircleShape
                        )
                        .align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D3748))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Column {
                Text(
                    text = member.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Text
                )
                Text(
                    text = stringResource(
                        id = if (member.isInside) R.string.inside else R.string.outside
                    ),
                    fontSize = 14.sp,
                    color = if (member.isInside) InLab else Outdoor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}