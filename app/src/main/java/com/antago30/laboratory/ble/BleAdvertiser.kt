package com.antago30.laboratory.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import java.util.UUID

class BleAdvertiser(
    context: Context,
    private val serviceUuid: UUID,
    private val adDataString: String = "J7h56Ak98g"
) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser

    private var isAdvertising = false
    var onStateChanged: ((Boolean) -> Unit)? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            onStateChanged?.invoke(true)
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            onStateChanged?.invoke(false)
            android.util.Log.e("BleAdvertiser", "Advertising failed with error: $errorCode")
        }
    }

    fun startAdvertising() {
        if (!isSupported()) {
            onStateChanged?.invoke(false)
            return
        }
        if (isAdvertising) return

        val adDataBytes = adDataString.toByteArray(Charsets.UTF_8)

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(serviceUuid))
            .addServiceData(ParcelUuid(serviceUuid), adDataBytes)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setTimeout(0)
            .build()

        advertiser?.startAdvertising(settings, advertiseData, advertiseCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        onStateChanged?.invoke(false)
    }

    fun isSupported(): Boolean {
        return adapter != null &&
                adapter.isEnabled &&
                advertiser != null &&
                adapter.isMultipleAdvertisementSupported
    }
}