package com.antago30.laboratory.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    isBroadcasting: Boolean = false,
    onSettingsButtonClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var settingsScale by remember { mutableFloatStateOf(1f) }

    val animatedSettingsScale by animateFloatAsState(
        targetValue = settingsScale,
        label = "SettingsButtonScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isBroadcasting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.advertise),
                contentDescription = stringResource(R.string.startAdvertising),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                settingsScale = 0.7f
                scope.launch {
                    delay(150)
                    settingsScale = 1f
                }
                onSettingsButtonClick()
            },
            modifier = Modifier.scale(animatedSettingsScale)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.settings),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}