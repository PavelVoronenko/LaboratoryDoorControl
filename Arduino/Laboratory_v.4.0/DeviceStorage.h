#ifndef DEVICE_STORAGE_H
#define DEVICE_STORAGE_H

#include <Arduino.h>
#include <Preferences.h>
#include "Pins.h"

// -------------- Структура для хранения в NVS (минимальная) ------------
struct TrustedDeviceNVS {
  int id;                    // Уникальный ID (ключ)
  char name[64];             // Фиксированный размер для NVS
  char uuid[37];             // UUID формата: 00000000-0000-0000-0000-000000000000
  char serviceDataHex[32];   // Service data
  char macAddress[18];       // MAC: XX:XX:XX:XX:XX:XX
};

// ------------------ Структура доверенного устройства ------------------
struct TrustedDevice {
  int id;
  String name;
  String uuid;
  String serviceDataHex;
  String macAddress;
  String location; // "inside" или "outside"
  int rssiThreshold;  // порог RSSI для открытия
  uint64_t userTime;
  bool entryInProgress;
  bool exitInProgress;
};

// ------------------ Инициализация хранилища ------------------
void initStorage();

// ------------------ Загрузка всех пользователей из NVS в runtime-массив ------------------
int loadTrustedDevices(TrustedDevice* outArray, int maxSize);

// ------------------ Сохранение одного пользователя в NVS ------------------
bool saveTrustedDevice(const TrustedDeviceNVS* device);

// ------------------ Удаление пользователя по ID ------------------
bool deleteTrustedDevice(int id);

// ------------------ Поиск устройства по MAC ------------------
int findDeviceByMAC(const TrustedDevice* array, int count, const String& mac);

// ------------------ Поиск устройства по UUID + ServiceData ------------------
int findDeviceByUUID(const TrustedDevice* array, int count, const String& uuid, const String& serviceData);

// ------------------ Управление флагом перезагрузки ------------------
void setManualRebootFlag(bool status);
bool getManualRebootFlag();

// Глобальные переменные
extern Preferences usersPrefs;
extern TrustedDevice trustedDevices[];
extern int trustedDevicesCount;

#endif // DEVICE_STORAGE_H
