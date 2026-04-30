package com.antago30.laboratory.ble.bleConnectionManager

import android.util.Log
import com.antago30.laboratory.model.CharacteristicData
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class BleUserListHandler(
    private val coroutineScope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val connectionStateFlow: StateFlow<ConnectionState>,
    private val onSyncComplete: () -> Unit
) {
    private val _users = MutableStateFlow(settingsRepo.getCachedUserInfoList())
    val users: StateFlow<List<UserInfo>> = _users.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)

    private val userListChunks = mutableMapOf<Int, String>()
    private var isReceiving = false
    private var lastReceivedTime = 0L
    private val chunkTimeoutMs = 2000L

    init {
        // Сбрасываем буфер, если соединение разорвано
        coroutineScope.launch {
            connectionStateFlow.collect { state ->
                if (state == ConnectionState.DISCONNECTED) {
                    android.util.Log.d("BleUserListHandler", "Connection lost, resetting buffer")
                    resetBuffer()
                }
            }
        }
    }

    fun handleData(data: CharacteristicData) {
        val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
        
        if (response.startsWith("USERLIST_PKT:")) {
            handleChunk(response)
        } else if (response.startsWith("USERLIST:")) {
            parseAndSync(response.removePrefix("USERLIST:"))
        }
    }

    private fun handleChunk(packet: String) {
        try {
            val headerEnd = packet.indexOf('|')
            if (headerEnd == -1) return

            val header = packet.substring(13, headerEnd)
            val headerParts = header.split('/')
            val chunkIndex = headerParts[0].toInt()
            
            val dataStart = headerEnd + 1
            val hasEnd = packet.endsWith("|END")
            val dataEnd = if (hasEnd) packet.length - 4 else packet.length
            val chunkData = if (dataEnd <= dataStart) "" else packet.substring(dataStart, dataEnd)

            if (chunkIndex == 0) {
                userListChunks.clear()
                isReceiving = true
                _isSyncing.value = true
                startTimeoutCheck()
            }

            userListChunks[chunkIndex] = chunkData
            lastReceivedTime = System.currentTimeMillis()

            if (hasEnd) {
                assembleAndParse()
            }
        } catch (e: Exception) {
            Log.e("BleUserListHandler", "Error parsing chunk", e)
            resetBuffer()
        }
    }

    private fun assembleAndParse() {
        val fullData = userListChunks.toSortedMap().values.joinToString("")
        resetBuffer()
        parseAndSync(fullData)
        onSyncComplete()
    }

    private fun parseAndSync(data: String) {
        if (data.isBlank() || data == "|") {
            updateList(emptyList())
            return
        }

        val parsed = data.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size >= 6) {
                    val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val name = parts[1].trim()
                    val mac = parts[2].trim()
                    val location = parts[3].trim()
                    val uuid = parts[4].trim()
                    val serviceData = parts[5].trim()
                    
                    // Расширенные поля (RSSI) если есть
                    val rssiEntry = parts.getOrNull(6)?.toIntOrNull() ?: -70
                    val rssiExit = parts.getOrNull(7)?.toIntOrNull() ?: -70

                    UserInfo(
                        id = id,
                        name = name,
                        macAddress = mac,
                        location = location,
                        uuid = uuid,
                        serviceData = serviceData,
                        rssiThresholdEntry = rssiEntry,
                        rssiThresholdExit = rssiExit
                    )
                } else null
            }
            .distinctBy { it.id }

        updateList(parsed)
        _isSyncing.value = false
    }

    private fun updateList(newList: List<UserInfo>) {
        if (_users.value != newList) {
            _users.value = newList
            settingsRepo.saveCachedUserInfoList(newList)
        }
    }

    private fun resetBuffer() {
        userListChunks.clear()
        isReceiving = false
        _isSyncing.value = false
    }

    private fun startTimeoutCheck() {
        coroutineScope.launch {
            delay(chunkTimeoutMs)
            if (isReceiving && System.currentTimeMillis() - lastReceivedTime >= chunkTimeoutMs) {
                if (userListChunks.isNotEmpty()) {
                    assembleAndParse()
                } else {
                    resetBuffer()
                }
            }
        }
    }
}
