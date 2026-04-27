#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <BLEScan.h>
#include <BLEClient.h>
#include "Pins.h"
#include "DeviceStorage.h"

// ------------------ Структура обнаруженных устройств ------------------
struct DevicesDetected {
  String name;
  int rssi;
};

// ------------------ Глобальные переменные BLE ------------------
extern BLEServer* pServer;
extern BLECharacteristic* pCharacteristic;
extern BLECharacteristic* Terminal;
extern BLECharacteristic* DebugChar;
extern BLEScan* pBLEScan;
extern BLEClient *pClient;
extern BLERemoteCharacteristic* pRemoteCharacteristic;

extern bool connected;
extern bool jdeConnect;
extern String rxValue;
extern String lightStatus;
extern bool autoLightOutside;
extern unsigned long SendCommandTime;

extern DevicesDetected devicesDetected[10];

// ------------------ Инициализация BLE сервера ------------------
void initBLEServer();
void updateAdvertising();

// ------------------ Сканирование и подключение к JDY-33 ------------------
bool scanAndConnect();

// ------------------ Сканирование доверенных устройств ------------------
void scanForTrustedDevices();

// ------------------ Отправка уведомлений через терминал ------------------
enum LogType {
    LOG_INFO,   // [I] Системные сообщения (подключение/отключение)
    LOG_DOOR,   // [D] Двери (открытие/закрытие)
    LOG_USER,   // [U] Пользователи (вход/выход)
    LOG_WARN    // [W] Предупреждения/ошибки
};
void log(String message, LogType type = LOG_INFO);

// ------------------ Отправка служебных данных ------------------
void sendCommand();
void sendDebugData(float distance, int threshold, int doorTime, int doorCooldown);

// ------------------ Отправка списка пользователей ------------------
void sendUserListChunked();

// ------------------ Отправка истории логов ------------------
void sendLogHistoryChunked();
void addToLogHistory(String message);

// ------------------ Обработка входа/выхода (определено в Utils) ------------------
void processExitAndEntry(DevicesDetected *devDetected);

// ------------------ Проверка статуса входа/выхода (определено в Utils) ------------------
void checkEntryExitStatus();

// ------------------ Проверка людей внутри (определено в Utils) ------------------
void checkPeopleInside();

#endif // BLE_MANAGER_H
