package com.antago30.laboratory.ui.component.settingsScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antago30.laboratory.R
import com.antago30.laboratory.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scope = rememberCoroutineScope()
    var backScale by remember { mutableFloatStateOf(1f) }
    var addUserScale by remember { mutableFloatStateOf(1f) }

    val animatedBackScale by animateFloatAsState(
        targetValue = backScale,
    )
    val animatedAddUserScale by animateFloatAsState(
        targetValue = addUserScale,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = {
                backScale = 0.7f
                scope.launch {
                    delay(150)
                    backScale = 1f
                }
                onBack()
            },
            modifier = Modifier.scale(animatedBackScale)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                //tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            "Настройки",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = {
                addUserScale = 0.7f
                scope.launch {
                    delay(150)
                    addUserScale = 1f
                }
                //onBack()
            },
            modifier = Modifier.scale(animatedAddUserScale)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_group_add_24),
                contentDescription = stringResource(R.string.addUser),
                //tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}