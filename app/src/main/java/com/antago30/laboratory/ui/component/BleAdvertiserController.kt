package com.antago30.laboratory.ui.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.antago30.laboratory.ble.BleAdvertiser

@Composable
fun BleAdvertiserController(
    isBroadcasting: Boolean,
    onBroadcastingSupported: (Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    val bleAdvertiser = remember(context) {
        BleAdvertiser(context)
    }

    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            if (!bleAdvertiser.isSupported()) {
                onBroadcastingSupported(false)
                return@LaunchedEffect
            }
            @Suppress("MissingPermission")
            bleAdvertiser.startAdvertising("J7hs2Ak98g")
            onBroadcastingSupported(true)
        } else {
            @Suppress("MissingPermission")
            bleAdvertiser.stopAdvertising()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            @Suppress("MissingPermission")
            bleAdvertiser.stopAdvertising()
        }
    }
}